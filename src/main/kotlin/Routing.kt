package com.example

import com.example.model.Priority
import com.example.model.Task
import com.example.views.auth.LoginView
import com.example.views.auth.RegisterView
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.http.content.staticResources
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        staticResources("static", "static")

//        authenticate("auth-jwt") {
//            helloView()
//        }

        RegisterView()

        LoginView()
//        get("/") {
//            call.respondText("Hello, World!")
//        }
//        get("/json/kotlinx-serialization") {
//            call.respond(mapOf("hello" to "world"))
//        }
//        get("/tasks") {
//            call.respond(listOf(
//                Task("test", "test", Priority.Medium),
//            ))
//        }
    }
}
