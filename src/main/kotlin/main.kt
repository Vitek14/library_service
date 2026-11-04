package com.example

import com.example.config.AppConfig
import com.example.db.DatabaseFactory
import com.example.db.repository.AuthorRepository
import com.example.db.repository.BookRepository
import com.example.db.repository.GenreRepository
import com.example.db.repository.RentRequestRepository
import com.example.db.repository.UserRepository
import io.github.cdimascio.dotenv.Dotenv
import io.github.cdimascio.dotenv.dotenv
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped

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

    val authorRepository = AuthorRepository()
    val genreRepository  = GenreRepository()
    val bookRepository   = BookRepository(authorRepository, genreRepository)
    val userRepository   = UserRepository()
    val rentRequestRepository = RentRequestRepository()
}
