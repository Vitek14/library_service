package com.example

import com.example.model.Priority
import com.example.model.Task
import com.example.views.auth.LoginView
import com.example.views.auth.RegisterView
import com.example.views.book.CreateBook
import com.example.views.book.CreateBookAuthor
import com.example.views.book.CreateBookGenre
import com.example.views.book.ListBooksView
import com.example.views.rent.ListRentView
import com.example.views.rent.RentApproveView
import com.example.views.rent.RentBookView
import com.example.views.rent.RentRejectView
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.http.content.staticResources
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        authenticate("auth-jwt") {
            CreateBookAuthor()
            CreateBookGenre()
            CreateBook()
            ListRentView()
            RentBookView()
            RentApproveView()
            RentRejectView()
        }

        RegisterView()

        LoginView()

        ListBooksView()
    }
}
