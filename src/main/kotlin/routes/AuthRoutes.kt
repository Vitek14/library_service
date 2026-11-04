package com.example.routes
import com.example.model.dto.auth.LoginRequest
import com.example.model.dto.auth.RegisterRequest
import com.example.service.AuthService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes(authService: AuthService) {
    post("/auth/register") {
        val req = call.receive<RegisterRequest>()
        call.respond(HttpStatusCode.Created, authService.register(req))
    }
    post("/auth/login") {
        val req = call.receive<LoginRequest>()
        call.respond(HttpStatusCode.OK, authService.login(req))
    }
}
