package moe.luminolmc

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.mongodb.client.MongoDatabase
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.config.*
import io.ktor.server.plugins.cachingheaders.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import moe.luminolmc.routes.initDownloadsRoute
import moe.luminolmc.routes.initProjectsRoute

lateinit var mongoDatabase: MongoDatabase

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    install(ContentNegotiation) {
        gson {
        }
    }
    install(Compression)
    install(CachingHeaders) {
        options { call, outgoingContent ->
            when (outgoingContent.contentType?.withoutParameters()) {
                ContentType.Text.CSS -> CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 24 * 60 * 60))
                else -> null
            }
        }
    }
    val expectedToken = environment.config.tryGetString("auth.token")
    install(Authentication) {
        bearer("auth-bearer") {
            authenticate { tokenCredential ->
                if (tokenCredential.token == expectedToken) {
                    UserIdPrincipal("StarFall")
                } else { null }
            }
        }
    }
    mongoDatabase = connectToMongoDB()
    configAPIs()
}

fun Application.configAPIs() {
    routing {
        get("/") {
            call.respond("Hello!")
        }
    }

    initProjectsRoute()
    initDownloadsRoute()
}