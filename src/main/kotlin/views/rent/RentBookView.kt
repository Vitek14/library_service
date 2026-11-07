package com.example.views.rent

import com.example.db.services.createRentRequest
import com.example.db.services.getUserById
import com.example.models.rent.RentRequest
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.jsonSchema
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.post
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun Route.RentBookView() {
    post("/rent-requests/{id}") {
        val id: String? = call.parameters["id"]

        val requestId = call.parameters["id"]?.toLongOrNull()
            ?: return@post call.respond(HttpStatusCode.BadRequest)
        val userId = call.principal<JWTPrincipal>()
            ?.payload?.getClaim("id")?.asLong()
            ?: return@post call.respond(HttpStatusCode.Unauthorized)
        val user = getUserById(userId)

        val rentStatus = createRentRequest(requestId, userId)
        call.respond(rentStatus)
    }.describe {
        summary = "Creates a new rent request on book"
        parameters {
            query("id") {
                description = "Id of the book"
                required = true
            }
        }
        responses {
            HttpStatusCode.OK {
                description = "Rent requested created"
                schema = jsonSchema<RentRequest>()
            }
            HttpStatusCode.Unauthorized
            HttpStatusCode.BadRequest
            HttpStatusCode.Forbidden
        }
    }
}