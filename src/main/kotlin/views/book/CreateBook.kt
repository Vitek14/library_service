package com.example.views.book

import com.example.Serializers.books.AuthorBookRequest
import com.example.Serializers.books.BookRequest
import com.example.db.services.createAuthor
import com.example.db.services.createBook
import com.example.db.services.getUserById
import com.example.models.book.Author
import com.example.models.book.Book
import com.example.models.user.Roles
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.jsonSchema
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.post

fun Route.CreateBook() {
    post("/books") {
        val body = call.receive<BookRequest>()
        val userId = call.principal<JWTPrincipal>()
            ?.payload?.getClaim("id")?.asLong()
            ?: return@post call.respond(HttpStatusCode.Unauthorized)
        if (body.title.isBlank() || body.authors.isEmpty() || body.genres.isEmpty()) {
            call.respond(HttpStatusCode.BadRequest, "please enter a not empty data")

            return@post
        }
        val user = getUserById(userId)
        if (user.role != Roles.ADMIN && user.role != Roles.OWNER) {
            call.respond(HttpStatusCode.Forbidden, "Access denied")
            return@post
        }

        val createdBook = createBook(body.title, body.authors, body.genres)
        call.respond(createdBook)
    }.describe {
        summary = "Creates a new Book"
        requestBody {
            schema = jsonSchema<BookRequest>()
            required = true
        }
        responses {
            HttpStatusCode.OK {
                description = "Book created"
                schema = jsonSchema<Book>()
            }
            HttpStatusCode.Unauthorized
            HttpStatusCode.BadRequest
            HttpStatusCode.Forbidden
        }
    }
}