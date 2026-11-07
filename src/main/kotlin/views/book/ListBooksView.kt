package com.example.views.book

import com.example.Serializers.books.ListBooksResponse
import com.example.db.services.getBooks
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.jsonSchema
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe

fun Route.ListBooksView(){
    get("/books") {
        val books = getBooks()
        call.respond(ListBooksResponse(books))
    }.describe {
        summary = "List Books"
        responses {
            HttpStatusCode.OK {
                schema = jsonSchema<ListBooksResponse>()
            }
        }
    }
}