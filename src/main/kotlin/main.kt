package com.example

import io.github.cdimascio.dotenv.dotenv

fun main(args: Array<String>) {
    val dotenv = dotenv {
        ignoreIfMalformed = true
        ignoreIfMissing = true
    }
    System.setProperty("JWT_SECRET", dotenv["JWT_SECRET"])

    io.ktor.server.netty.EngineMain.main(args)
}
