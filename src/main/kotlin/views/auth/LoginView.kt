package com.example.views.auth

import com.example.Serializers.auth.AuthResponse
import com.example.Serializers.auth.AuthException
import com.example.Serializers.auth.LoginRequest
import com.example.db.services.getUserByLogin
import com.example.utilities.auth.PasswordHasher.verifyPassword
import com.example.utilities.auth.generateJWT
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.jsonSchema
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.post
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun Route.LoginView() {
    post("/login") {
        val body = call.receive<LoginRequest>()

        if (body.login.isEmpty() || body.password.isEmpty()) {
            call.respond(HttpStatusCode.BadRequest, "please enter a login and a password")

            return@post
        }

        val storedUser = getUserByLogin(body.login)

        val isValidPassword = verifyPassword(storedUser.password, body.password.toCharArray())
        if (isValidPassword) {
            val token = generateJWT(storedUser)
            call.respond(AuthResponse(
                accessToken = token.token,
                expiresAt = token.expiresAt
            ))
        } else {
            call.respond(HttpStatusCode.BadRequest, "login or password invalid")
        }
    }.describe {
        summary = "Authenticates user"
        requestBody {
            schema = jsonSchema<LoginRequest>()
            description = "User payload"
            required = true
        }
        responses {
            HttpStatusCode.OK {
                schema = jsonSchema<AuthResponse>()
            }
            HttpStatusCode.InternalServerError {
                description = "Failed to authenticate"
                schema = jsonSchema<AuthException>()
            }
        }
    }
}
