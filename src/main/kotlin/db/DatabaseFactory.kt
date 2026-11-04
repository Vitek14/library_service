package com.example.db

import com.example.config.AppConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.sql.Connection
import javax.sql.DataSource

object DatabaseFactory : Closeable {
    private val log = LoggerFactory.getLogger(javaClass)
    private lateinit var hikariDataSource: HikariDataSource

    fun init(appConfig: AppConfig) {
        val hikariConfig = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"  // maybe move to config?
            jdbcUrl = appConfig.dbJdbcUrl
            username = appConfig.dbUser
            password = appConfig.dbPassword
            maximumPoolSize = appConfig.dbMaxPoolSize
            minimumIdle = 2
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            poolName = "library-pool"
            validate()
        }
        hikariDataSource = HikariDataSource(hikariConfig)
        log.info("HikariCP initialized: url={}, poolSize={}", appConfig.dbJdbcUrl, appConfig.dbMaxPoolSize)
        applySchema()
    }

    fun dataSource(): DataSource = hikariDataSource

    /*
    jdbc calls are with Dispatcher.IO to not block netty(event loop)
     */
    suspend fun <T> withTransaction(block: (Connection) -> T): T = withContext(Dispatchers.IO) {
        hikariDataSource.connection.use { conn ->
            conn.autoCommit = false
            try {
                val result = block(conn)
                conn.commit()
                result
            } catch (e: Throwable) {
                conn.rollback()
                throw e
            }
        }
    }

    private fun applySchema() {
        val sql = javaClass.classLoader.getResourceAsStream("db/schema.sql")
            ?.bufferedReader()?.use { it.readText() }
            ?: error("schema.sql not found in resources/db/")

        hikariDataSource.connection.use { conn ->
            conn.autoCommit = true
            conn.createStatement().use { stmt ->
                stmt.execute(sql)
            }
        }
        log.info("Database schema applied successfully")
    }

    override fun close() {
        if (::hikariDataSource.isInitialized && !hikariDataSource.isClosed) {
            hikariDataSource.close()
            log.info("HikariCP pool closed")
        }
    }
}
