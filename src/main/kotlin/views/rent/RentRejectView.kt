package com.example.views.rent

import com.example.db.services.getUserById
import com.example.db.services.setRentStatus
import com.example.models.rent.RentRequest
import com.example.models.rent.Status
import com.example.models.user.Roles
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.jsonSchema
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.post
import kotlin.text.toLongOrNull

fun Route.RentRejectView () {
    post("/rent-requests/reject/{id}") {
        val id: String? = call.parameters["id"]
        val requestId = call.parameters["id"]?.toLongOrNull()
            ?: return@post call.respond(HttpStatusCode.BadRequest)

        val userId = call.principal<JWTPrincipal>()
            ?.payload?.getClaim("id")?.asLong()
            ?: return@post call.respond(HttpStatusCode.Unauthorized)
        val user = getUserById(userId)
        if (user.role != Roles.ADMIN && user.role != Roles.OWNER) {
            call.respond(HttpStatusCode.Forbidden, "Access denied")
            return@post
        }
        val approvedRequest = setRentStatus(requestId, Status.REJECTED)
        call.respond(approvedRequest)
    }.describe {
        summary = "Reject Rent Request"
        parameters {
            query("id") {
                description = "Id of the book"
                required = true
            }
        }
        responses {
            HttpStatusCode.OK {
                schema = jsonSchema<RentRequest>()
            }
        }
        HttpStatusCode.Unauthorized
        HttpStatusCode.Forbidden
    }
}
