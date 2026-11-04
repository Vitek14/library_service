package com.example.config

import io.ktor.server.config.*

class AppConfig(config: ApplicationConfig) {
    val dbJdbcUrl: String = config.property("database.jdbcUrl").getString()
    val dbUser: String = config.property("database.user").getString()
    val dbPassword: String = config.property("database.password").getString()
    val dbMaxPoolSize: Int = config.property("database.maxPoolSize").getString().toInt()

    val jwtSecret: String = config.property("jwt.secret").getString()
    val jwtIssuer: String = config.property("jwt.issuer").getString()
    val jwtAudience: String = config.property("jwt.audience").getString()
    val jwtRealm: String = config.property("jwt.realm").getString()
    val jwtExpirationMs: Long = config.property("jwt.expirationMs").getString().toLong()

    val defaultAdminLogin: String = config.property("defaultAdmin.login").getString()
    val defaultAdminPassword: String = config.property("defaultAdmin.password").getString()
    val defaultAdminName: String = config.property("defaultAdmin.name").getString()
    val defaultAdminSurname: String = config.property("defaultAdmin.surname").getString()
}
