package com.example.views.rent

import com.example.Serializers.books.ListBooksResponse
import com.example.Serializers.rent.ListRentResponse
import com.example.db.services.getAllRentRequests
import com.example.db.services.getBooks
import com.example.db.services.getUserById
import com.example.models.user.Roles
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.jsonSchema
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe

fun Route.ListRentView(){
    get("/rent-requests") {
        val userId = call.principal<JWTPrincipal>()
            ?.payload?.getClaim("id")?.asLong()
            ?: return@get call.respond(HttpStatusCode.Unauthorized)
        val user = getUserById(userId)
        if (user.role != Roles.ADMIN && user.role != Roles.OWNER) {
            call.respond(HttpStatusCode.Forbidden, "Access denied")
            return@get
        }
        val requests = getAllRentRequests()
        call.respond(ListRentResponse(requests))
    }.describe {
        summary = "List Rent Requests"
        responses {
            HttpStatusCode.OK {
                schema = jsonSchema<ListRentResponse>()
            }
        }
        HttpStatusCode.Unauthorized
        HttpStatusCode.Forbidden
    }
}
