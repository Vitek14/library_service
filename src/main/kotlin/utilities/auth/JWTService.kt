package com.example.utilities.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.models.user.User
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respond
import java.util.Date
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

val secret: String? = System.getProperty("JWT_SECRET")

// setup jwt
fun Application.configureSecurity() {
    val jwtAudience = "jwt-audience"
    val jwtDomain = "https://jwt-provider-domain/"
    val jwtRealm = "ktor sample app"
    val jwtSecret = secret
    authentication {
        jwt("auth-jwt") {
            realm = jwtRealm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtDomain)
                    .build()
            )
            validate { credential ->
                val payload = credential.payload
                val claim = payload.getClaim("id")
                if (claim.asLong() != null && claim.asLong() > 0) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { defaultScheme, realm ->
                val text = "Token is not valid or has expired"
                val status = HttpStatusCode.Unauthorized
                call.respond(status, text)
            }
        }
    }
}

data class IssuedToken(
    val token: String,
    val expiresAt: Long,
)

@OptIn(ExperimentalTime::class)
fun generateJWT(user: User): IssuedToken {
    val expiresAt = System.currentTimeMillis() + 600_000L
    val token = JWT.create()
        .withAudience("jwt-audience")
        .withClaim("id", user.id)
        .withIssuer("https://jwt-provider-domain/")
        .withExpiresAt(Date(expiresAt))
        .sign(Algorithm.HMAC256(secret))
    return IssuedToken(token, expiresAt)
}
