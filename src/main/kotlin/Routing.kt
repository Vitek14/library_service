package com.example

import com.example.model.Priority
import com.example.model.Task
import com.example.views.auth.LoginView
import com.example.views.auth.RegisterView
import com.example.views.book.CreateBook
import com.example.views.book.CreateBookAuthor
import com.example.views.book.CreateBookGenre
import com.example.views.book.ListBooksView
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.http.content.staticResources
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        staticResources("static", "static")

        authenticate("auth-jwt") {
            CreateBookAuthor()
            CreateBookGenre()
            CreateBook()
        }

        RegisterView()

        LoginView()

        ListBooksView()
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
