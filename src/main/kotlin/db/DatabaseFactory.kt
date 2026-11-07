package com.example.db

import com.example.db.services.registerUser
import com.example.models.user.Roles
import com.example.utilities.auth.PasswordHasher.hashPassword
import com.example.utilities.envOrProperty
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.JarURLConnection
import java.util.jar.JarFile
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
        jdbcUrl  = envOrProperty("JDBC_URL")
        username = envOrProperty("DB_USER")
        password = envOrProperty("DB_PASSWORD")
        maximumPoolSize = 10
        isAutoCommit = false
    }
    return HikariDataSource(hikariConfig)
}

private fun runSchema(dataSource: DataSource) {
    val classLoader = object {}.javaClass.classLoader

    val orderedDirs = listOf(
        "db/schema/users",
        "db/schema/books",
        "db/schema/rent",
        "db/schema/mock",
    )

    val scripts = orderedDirs.flatMap { dir -> readSqlScripts(classLoader, dir) }

    dataSource.connection.use { conn ->
        conn.autoCommit = true
        scripts.forEach { (path, sql) ->
            conn.createStatement().use { it.execute(sql) }
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

private fun readSqlScripts(classLoader: ClassLoader, dir: String): List<Pair<String, String>> {
    val url = classLoader.getResource(dir) ?: return emptyList()

    return when (url.protocol) {
        "file" -> {
            File(url.toURI()).walk()
                .filter { it.isFile && it.extension == "sql" }
                .sortedBy { it.name }
                .map { file -> "$dir/${file.name}" to file.readText() }
                .toList()
        }
        "jar" -> {
            val jarFile = (url.openConnection() as JarURLConnection).jarFile
            readFromJar(jarFile, dir)
        }
        else -> error("Unsupported protocol: ${url.protocol} for $dir")
    }
}

private fun readFromJar(jar: JarFile, dir: String): List<Pair<String, String>> {
    val prefix = if (dir.endsWith("/")) dir else "$dir/"
    return jar.entries().asSequence()
        .filter { !it.isDirectory && it.name.startsWith(prefix) && it.name.endsWith(".sql") }
        .sortedBy { it.name }
        .map { entry ->
            val text = jar.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() }
            entry.name to text
        }
        .toList()
}
