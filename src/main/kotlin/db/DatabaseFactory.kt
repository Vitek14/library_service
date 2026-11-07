package com.example.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.sql.DataSource

private lateinit var dataSource: HikariDataSource

fun Application.configureDB() {
    // Specifies db init with hikari, checking that schema is already in db and gives control to an app

    dataSource = createDataSource()

    runSchema(dataSource)

    monitor.subscribe(ApplicationStopped) { dataSource.close() }
}

private fun createDataSource(): HikariDataSource {
    val hikariConfig = HikariConfig().apply {
        driverClassName = "org.postgresql.Driver"
        jdbcUrl = System.getProperty("JDBC_URL")
        username = System.getProperty("DB_USER")
        password = System.getProperty("DB_PASSWORD")
        maximumPoolSize = 10
        isAutoCommit = false
    }
    return HikariDataSource(hikariConfig)
}

private fun runSchema(dataSource: DataSource) {
    // read all *.sql files from resources/db/schema/* and execute them in order
    val classLoader = object {}.javaClass.classLoader

    val schemaFiles = listOf("db/schema/users", "db/schema/books")
        .flatMap { dir ->
            classLoader.getResource(dir)?.let { url ->
                File(url.toURI())
                    .walk()
                    .filter { it.isFile && it.extension == "sql" }
                    .sortedBy { it.name }
                    .toList()
            } ?: emptyList()
        }

    dataSource.connection.use { conn ->
        conn.autoCommit = true
        schemaFiles.forEach { file ->
            conn.createStatement().use { it.execute(file.readText()) }
        }
    }
}

suspend fun <T> dbQuery(block: (Connection) -> T): T = withContext(Dispatchers.IO) {
    // used to make unblocking connections in the app
    dataSource.connection.use { conn ->
        try {
            val result = block(conn)
            conn.commit()
            result
        } catch (t: Throwable) {
            runCatching { conn.rollback() }
            throw t
        }
    }
}
