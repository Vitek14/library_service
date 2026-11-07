package com.example

import io.github.cdimascio.dotenv.dotenv

fun main(args: Array<String>) {
    val dotenv = dotenv {
        ignoreIfMalformed = true
        ignoreIfMissing = true
    }
    System.setProperty("JWT_SECRET", dotenv["JWT_SECRET"])
    System.setProperty("JDBC_URL", dotenv["JDBC_URL"])
    System.setProperty("DB_USER", dotenv["DB_USER"])
    System.setProperty("DB_PASSWORD", dotenv["DB_PASSWORD"])

    io.ktor.server.netty.EngineMain.main(args)
}
