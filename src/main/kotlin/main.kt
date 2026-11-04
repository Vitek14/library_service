package com.example

import com.example.config.AppConfig
import com.example.db.DatabaseFactory
import com.example.db.repository.AuthorRepository
import com.example.db.repository.BookRepository
import com.example.db.repository.GenreRepository
import com.example.db.repository.RentRequestRepository
import com.example.db.repository.UserRepository
import com.example.plugins.configureSecurity
import com.example.routes.authRoutes
import com.example.security.JwtService
import com.example.security.PasswordHasher
import com.example.service.AuthService
import io.github.cdimascio.dotenv.Dotenv
import io.github.cdimascio.dotenv.dotenv
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    // loading env to current session and all variables, so app doesn't fail when parsing env
    dotenv { ignoreIfMissing = true }.entries().forEach { entry ->
        System.setProperty(entry.key, entry.value)
    }

    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val appConfig = AppConfig(environment.config)
    DatabaseFactory.init(appConfig)
    monitor.subscribe(ApplicationStopped) { DatabaseFactory.close() }

    val jwtService = JwtService(appConfig)
    configureSecurity(jwtService)

    val passwordHasher = PasswordHasher()
    val userRepository = UserRepository()
    val authService = AuthService(userRepository, passwordHasher, jwtService)

    val authorRepository = AuthorRepository()
    val genreRepository  = GenreRepository()
    val bookRepository   = BookRepository(authorRepository, genreRepository)
    val rentRequestRepository = RentRequestRepository()

    runBlocking {
        authService.bootstrapAdmin(
            login = appConfig.defaultAdminLogin,
            password = appConfig.defaultAdminPassword,
            name = appConfig.defaultAdminName,
            surname = appConfig.defaultAdminSurname,
        )
    }

    routing {
        authRoutes(authService)
    }
}
