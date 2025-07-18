package moe.luminolmc.routes

import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.origin
import io.ktor.server.response.*
import io.ktor.server.routing.*
import moe.luminolmc.data.CommitData
import moe.luminolmc.data.CommitsDatabaseService
import moe.luminolmc.data.ProjectData
import moe.luminolmc.data.ProjectsDatabaseService
import moe.luminolmc.mongoDatabase

val projectsDatabaseService = ProjectsDatabaseService(mongoDatabase)
val commitsDatabaseService = CommitsDatabaseService(mongoDatabase, projectsDatabaseService)

fun Application.initProjectsRoute() {
    routing {
        authenticate("auth-bearer") {
            post("projects/append_commits") {
                // TODO Auth Checks
                val projectName = call.parameters["project_name"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                val commitDataJsonArray = call.parameters["commit_data_json_array"] ?: return@post call.respond(HttpStatusCode.BadRequest)

                val decodedCommits: List<CommitData>

                try {
                    decodedCommits = JsonParser.parseString(commitDataJsonArray).asJsonArray.let {
                        val objects = mutableListOf<CommitData>()

                        for (obj in it) {
                            objects.add(CommitData.fromJsonObject(obj.asJsonObject))
                        }

                        return@let objects
                    }
                }catch (e: JsonSyntaxException){
                    log.error("Failed to parse commit data $commitDataJsonArray from user of Ip ${call.request.origin.remoteAddress}", e)
                    return@post call.respond(HttpStatusCode.BadRequest)
                }

                for (commit in decodedCommits) {
                    commitsDatabaseService.add(projectName, commit)
                }

                return@post call.respond(HttpStatusCode.OK)
            }

            post("/projects/create") {
                val arguments = call.parameters
                val projectName = arguments["project_name"] ?: return@post call.respond(HttpStatusCode.BadRequest)

                log.info("User from Ip address: ${call.request.origin.remoteAddress} is creating project $projectName")

                if (projectsDatabaseService.hasProject(projectName)) {
                    return@post call.respond(HttpStatusCode.BadRequest)
                }

                val created = ProjectData(projectName, emptyList())
                projectsDatabaseService.create(created)

                call.respond(HttpStatusCode.OK, created)
            }
        }

        get("/projects/get_commits") {
            val targetProjectName = call.parameters["project_name"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val targetBranchName = call.parameters["branch"] ?: return@get call.respond(HttpStatusCode.BadRequest)

            val commits = commitsDatabaseService.getCommitsOf(targetProjectName, targetBranchName)
            call.respond(HttpStatusCode.OK, commits)
        }

        get ("/projects") {
            val projectName = call.parameters["project_name"]

            // no arguments provided
            if (projectName == null) {
                val projects: List<String> = projectsDatabaseService.getAllProjects().map { it.name }

                return@get call.respond(HttpStatusCode.OK, projects)
            }

            val result = projectsDatabaseService.get(projectName)

            // no target project
            if (result == null) {
                return@get call.respond(HttpStatusCode.NotFound)
            }

            return@get call.respond(HttpStatusCode.OK, result)
        }
    }
}