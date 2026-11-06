package com.example.views

import com.example.Serializers.auth.AuthResponse
import com.example.Serializers.auth.RegisterRequest
import com.example.Serializers.auth.Serializers.auth.LoginRequest
import com.example.db.services.getUserByLogin
import com.example.db.services.registerUser
import com.example.models.user.Roles
import com.example.models.user.User
import com.example.utilities.auth.PasswordHasher.hashPassword
import com.example.utilities.auth.PasswordHasher.verifyPassword
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

        if (body.login.isEmpty() || body.name.isEmpty() || body.surname.isEmpty() || body.password.isEmpty()) {
            call.respond(HttpStatusCode.BadRequest, "please enter a login, name, surname and a password")

            return@post
        }

        // TODO: password hashing and other checks before inserting in db
        val hashedPassword = hashPassword(body.password.toCharArray())
        val created_user = registerUser(body.login, body.name, body.surname, hashedPassword, Roles.USER)

        val token = generateJWT(created_user)

        call.respond(AuthResponse(
            accessToken = token.token,
            expiresAt = token.expiresAt
        ))
    }
}

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
    }
}

fun Route.helloView() {
    get("/hello") {
        call.respondText("Hello from view!")
    }
}
