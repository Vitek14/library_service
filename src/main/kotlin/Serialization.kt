package com.example

import io.ktor.server.application.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation

fun Application.configureSerialization() {
    log.info("ContentNegotiation plugin is being installed")
    install(ContentNegotiation) {
        json()
    }
}