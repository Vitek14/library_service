package com.example.views

import com.example.Serializers.auth.AuthResponse
import com.example.Serializers.auth.RegisterRequest
import com.example.models.user.Roles
import com.example.models.user.User
import com.example.utilities.auth.generateJWT
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun Route.RegisterView() {
    post("/register") {
        val body = call.receive<RegisterRequest>()

        if (body.name.isEmpty() || body.surname.isEmpty() || body.password.isEmpty()) {
            call.respond(HttpStatusCode.BadRequest, "please enter a name, surname and a password")

            return@post
        }

        val fakeUser = User(
            id = 1,
            name = body.name,
            surname = body.surname,
            role = Roles.USER,
            password = body.password,
        )

        val token = generateJWT(fakeUser)

        call.respond(AuthResponse(
            accessToken = token.token,
            expiresAt = token.expiresAt
        ))
    }
}

fun Route.helloView() {
    get("/hello") {
        call.respondText("Hello from view!")
    }
}
