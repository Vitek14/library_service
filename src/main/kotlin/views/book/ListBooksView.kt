package com.example.views.book

import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.ListBooksView(){
    get("/books/{id}") {
        val bookId = call.parameters["id"]?.toIntOrNull()

    }
}