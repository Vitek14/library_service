package com.example.plugins

import com.example.model.enums.Role
import com.example.security.JwtPrincipal
import com.example.security.JwtService
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import java.nio.file.AccessDeniedException

const val JWT_AUTH = "jwt-auth"

fun Application.configureSecurity(jwtService: JwtService) {
    install(Authentication) {
        jwt(JWT_AUTH) {
            realm = "library"
            verifier(jwtService.verifier())
            validate { credential ->
                val userId = credential.payload.subject?.toLongOrNull()
                    ?: return@validate null
                val login = credential.payload.getClaim("login").asString()
                    ?: return@validate null
                val role = credential.payload.getClaim("role").asString()
                    ?.let { runCatching { Role.valueOf(it) }.getOrNull() }
                    ?: return@validate null
                JwtPrincipal(userId, login, role)
            }
        }
    }
}

fun ApplicationCall.requireRole(vararg allowed: Role): JwtPrincipal {
    val principal = principal<JwtPrincipal>()
        ?: throw IllegalStateException("No JWT principal - route must be inside authenticate {}")
    if (principal.role !in allowed) throw AccessDeniedException("You do not have permission to perform this operation")
    return principal
}
