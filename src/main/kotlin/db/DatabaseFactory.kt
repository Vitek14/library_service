package com.example.db

import com.example.db.services.registerUser
import com.example.models.user.Roles
import com.example.utilities.auth.PasswordHasher.hashPassword
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

suspend fun Application.configureDB() {
    // Specifies db init with hikari, checking that schema is already in db and gives control to an app

    dataSource = createDataSource()

    runSchema(dataSource)

    val password = "1234"
    val hashedPassword = hashPassword(password.toCharArray())
    seedAdminIfMissing(hashedPassword)

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

    val schemaFiles = listOf("db/schema/users", "db/schema/books", "db/schema/rent", "db/schema/mock")
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

private suspend fun seedAdminIfMissing(hashedPassword: String) {
    val adminExists = dbQuery { conn ->
        conn.prepareStatement("SELECT 1 FROM users WHERE role = 'ADMIN' LIMIT 1").use { ps ->
            ps.executeQuery().use { it.next() }
        }
    }
    if (adminExists) return

    registerUser(
        login    = "admin",
        name     = "Admin",
        surname  = "Admin",
        password = hashedPassword,
        role     = Roles.ADMIN,
    )
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
