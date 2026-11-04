package com.example.db.repository

import com.example.db.DatabaseFactory.withTransaction
import com.example.model.entity.User
import com.example.model.enums.Role
import java.sql.ResultSet
import java.time.Instant

class UserRepository {

    suspend fun findById(id: Long): User? = withTransaction { conn ->
        conn.prepareStatement(SQL_FIND_BY_ID).use { stmt ->
            stmt.setLong(1, id)
            stmt.executeQuery().use { rs -> if (rs.next()) rs.toUser() else null }
        }
    }

    suspend fun findByLogin(login: String): User? = withTransaction { conn ->
        conn.prepareStatement(SQL_FIND_BY_LOGIN).use { stmt ->
            stmt.setString(1, login)
            stmt.executeQuery().use { rs -> if (rs.next()) rs.toUser() else null }
        }
    }

    suspend fun existsByLogin(login: String): Boolean = withTransaction { conn ->
        conn.prepareStatement(SQL_EXISTS_BY_LOGIN).use { stmt ->
            stmt.setString(1, login)
            stmt.executeQuery().use { rs -> rs.next() && rs.getBoolean(1) }
        }
    }

    suspend fun existsByRole(role: Role): Boolean = withTransaction { conn ->
        conn.prepareStatement(SQL_EXISTS_BY_ROLE).use { stmt ->
            stmt.setString(1, role.name)
            stmt.executeQuery().use { rs -> rs.next() && rs.getBoolean(1) }
        }
    }

    suspend fun create(
        name: String,
        surname: String,
        login: String,
        passwordHash: String,
        role: Role,
    ): User = withTransaction { conn ->
        conn.prepareStatement(SQL_INSERT, arrayOf("id")).use { stmt ->
            stmt.setString(1, name)
            stmt.setString(2, surname)
            stmt.setString(3, login)
            stmt.setString(4, passwordHash)
            stmt.setString(5, role.name)
            stmt.executeUpdate()
            stmt.generatedKeys.use { keys ->
                check(keys.next()) { "Failed to obtain generated id for user" }
                findByIdInConnection(conn, keys.getLong(1))
                    ?: error("User not found after insert, should never happen")
            }
        }
    }

    // Вспомогательный метод: тот же SELECT, но переиспользует текущее соединение
    // (нужен для create, чтобы не открывать вторую транзакцию)
    private fun findByIdInConnection(conn: java.sql.Connection, id: Long): User? {
        conn.prepareStatement(SQL_FIND_BY_ID).use { stmt ->
            stmt.setLong(1, id)
            stmt.executeQuery().use { rs -> return if (rs.next()) rs.toUser() else null }
        }
    }

    companion object {
        private const val SQL_FIND_BY_ID =
            "SELECT id, name, surname, login, password_hash, role, created_at, updated_at " +
            "FROM users WHERE id = ?"

        private const val SQL_FIND_BY_LOGIN =
            "SELECT id, name, surname, login, password_hash, role, created_at, updated_at " +
            "FROM users WHERE login = ?"

        private const val SQL_EXISTS_BY_LOGIN =
            "SELECT EXISTS(SELECT 1 FROM users WHERE login = ?)"

        private const val SQL_EXISTS_BY_ROLE =
            "SELECT EXISTS(SELECT 1 FROM users WHERE role = ?)"

        private const val SQL_INSERT =
            "INSERT INTO users (name, surname, login, password_hash, role) " +
            "VALUES (?, ?, ?, ?, ?) RETURNING id"
    }
}

private fun ResultSet.toUser(): User = User(
    id = getLong("id"),
    name = getString("name"),
    surname = getString("surname"),
    login = getString("login"),
    passwordHash = getString("password_hash"),
    role = Role.valueOf(getString("role")),
    createdAt = getTimestamp("created_at").toInstant(),
    updatedAt = getTimestamp("updated_at").toInstant(),
)
