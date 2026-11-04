package com.example.routes

import com.example.model.dto.rent.ApproveRentRequest
import com.example.model.dto.rent.CreateRentRequest
import com.example.model.enums.Role
import com.example.plugins.JWT_AUTH
import com.example.plugins.requireRole
import com.example.service.ApiException
import com.example.service.RentService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.Instant
import java.time.format.DateTimeParseException

fun Route.rentRoutes(rentService: RentService) {
    authenticate(JWT_AUTH) {

        post("/rent-requests") {
            val principal = call.requireRole(Role.USER, Role.ADMIN, Role.OWNER)
            val req = call.receive<CreateRentRequest>()
            call.respond(HttpStatusCode.Created, rentService.createRequest(principal.userId, req.bookId))
        }

        route("/admin/rent-requests") {
            get {
                call.requireRole(Role.ADMIN, Role.OWNER)
                call.respond(rentService.listPending())
            }
            post("/{id}/approve") {
                call.requireRole(Role.ADMIN, Role.OWNER)
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: throw ApiException(HttpStatusCode.BadRequest, "invalid request id")
                val body = call.receive<ApproveRentRequest>()
                val expiresAt = try {
                    Instant.parse(body.expiresAt)
                } catch (e: DateTimeParseException) {
                    throw ApiException(HttpStatusCode.BadRequest, "expiresAt must be ISO-8601 (e.g. 2026-11-10T12:00:00Z)")
                }
                call.respond(rentService.approve(id, expiresAt))
            }
            post("/{id}/decline") {
                call.requireRole(Role.ADMIN, Role.OWNER)
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: throw ApiException(HttpStatusCode.BadRequest, "invalid request id")
                call.respond(rentService.decline(id))
            }
        }
    }
}
