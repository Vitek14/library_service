package com.example.views.book

import com.example.Serializers.books.AuthorBookRequest
import com.example.db.services.createAuthor
import com.example.db.services.getUserById
import com.example.models.book.Author
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

fun Route.CreateBookAuthor() {
    post("/books/authors") {
        val body = call.receive<AuthorBookRequest>()
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

        val createdAuthor = createAuthor(body.name)
        call.respond(createdAuthor)
    }.describe {
        summary = "Creates a new Book Author"
        requestBody {
            schema = jsonSchema<AuthorBookRequest>()
            required = true
        }
        responses {
            HttpStatusCode.OK {
                description = "Author created"
                schema = jsonSchema<Author>()
            }
            HttpStatusCode.Unauthorized
            HttpStatusCode.BadRequest
            HttpStatusCode.Forbidden
        }
    }
}