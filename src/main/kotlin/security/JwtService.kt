package com.example.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.example.config.AppConfig
import com.example.model.entity.User
import java.time.Instant

class JwtService(appConfig: AppConfig) {
    private val algorithm = Algorithm.HMAC256(appConfig.jwtSecret)
    private val issuer = appConfig.jwtIssuer
    private val audience = appConfig.jwtAudience
    private val expirationMs = appConfig.jwtExpirationMs

    fun generateToken(user: User): String {
        val now = Instant.now()
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(user.id.toString())
            .withClaim("login", user.login)
            .withClaim("role", user.role.name)
            .withIssuedAt(now)
            .withExpiresAt(now.plusMillis(expirationMs))
            .sign(algorithm)
    }

    // maybe move to helpers
    fun expiresInSeconds(): Long = expirationMs / 1000

    fun verifier(): JWTVerifier = JWT
        .require(algorithm)
        .withIssuer(issuer)
        .withAudience(audience)
        .build()
}
