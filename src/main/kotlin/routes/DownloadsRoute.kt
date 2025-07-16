package moe.luminolmc.routes

import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import moe.luminolmc.data.CommitData
import moe.luminolmc.data.DownloadInfo
import moe.luminolmc.data.DownloadInfoDatabaseService
import moe.luminolmc.mongoDatabase

val downloadInfoDatabaseService = DownloadInfoDatabaseService(mongoDatabase)

fun Application.initDownloadsRoute() {
    routing {
        get("/downloads") {
            val branchName = call.parameters["branch_name"] ?: return@get call.respond(HttpStatusCode.BadRequest)

            val allDownloadInfos = downloadInfoDatabaseService.getAllOf(branchName)

            call.respond(HttpStatusCode.OK,allDownloadInfos)
        }

        get("downloads/create") {
            val branchName = call.parameters["branch_name"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val releaseTag = call.parameters["release_tag"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val mcVersion = call.parameters["mc_version"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val downloadLink = call.parameters["download_link"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val commitsJson = call.parameters["commits"] ?: return@get call.respond(HttpStatusCode.BadRequest)

            val decodedCommits: List<CommitData>

            try {
                decodedCommits = JsonParser.parseString(commitsJson).asJsonArray.let {
                    val objects = mutableListOf<CommitData>()

                    for (obj in it) {
                        objects.add(CommitData.fromJsonObject(obj.asJsonObject))
                    }

                    return@let objects
                }
            }catch (e: JsonSyntaxException){
                log.error("Failed to parse commit data $commitsJson from user of Ip ${call.request.origin.remoteAddress}", e)
                return@get call.respond(HttpStatusCode.BadRequest)
            }

            // TODO Auth check

            val created = DownloadInfo(releaseTag, branchName, mcVersion, decodedCommits, downloadLink)
            downloadInfoDatabaseService.add(created)

            call.respond(HttpStatusCode.OK)
        }
    }
}