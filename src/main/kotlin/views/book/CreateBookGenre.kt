package com.example.views.book

import com.example.Serializers.books.AuthorBookRequest
import com.example.Serializers.books.GenreBookRequest
import com.example.db.services.createAuthor
import com.example.db.services.createGenre
import com.example.db.services.getUserById
import com.example.models.book.Author
import com.example.models.book.Genre
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

fun Route.CreateBookGenre() {
    post("/books/genres") {
        val body = call.receive<GenreBookRequest>()
        val userId = call.principal<JWTPrincipal>()
            ?.payload?.getClaim("id")?.asLong()
            ?: return@post call.respond(HttpStatusCode.Unauthorized)
        if (body.name.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, "please enter author name")

            return@post
        }
        val user = getUserById(userId)
        if (user.role != Roles.ADMIN && user.role != Roles.OWNER) {
            call.respond(HttpStatusCode.Forbidden, "Access denied")
            return@post
        }

        val createdGenre = createGenre(body.name)
        call.respond(createdGenre)
    }.describe {
        summary = "Creates a new Book Genre"
        requestBody {
            schema = jsonSchema<GenreBookRequest>()
            required = true
        }
        responses {
            HttpStatusCode.OK {
                description = "Genre created"
                schema = jsonSchema<Genre>()
            }
            HttpStatusCode.Unauthorized
            HttpStatusCode.BadRequest
            HttpStatusCode.Forbidden
        }
    }
}