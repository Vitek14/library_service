package com.example.routes

import com.example.model.dto.book.CreateBookRequest
import com.example.model.enums.Role
import com.example.plugins.JWT_AUTH
import com.example.plugins.requireRole
import com.example.service.ApiException
import com.example.service.BookService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.bookRoutes(bookService: BookService) {
    route("/books") {
        get {
            call.respond(bookService.list())
        }
        get("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: throw ApiException(HttpStatusCode.BadRequest, "invalid book id")
            call.respond(bookService.getById(id))
        }
        authenticate(JWT_AUTH) {
            post {
                call.requireRole(Role.ADMIN, Role.OWNER)
                val req = call.receive<CreateBookRequest>()
                call.respond(HttpStatusCode.Created, bookService.create(req))
            }
        }
    }
}
