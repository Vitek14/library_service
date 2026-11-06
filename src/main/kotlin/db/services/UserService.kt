package com.example.db.services

import com.example.db.dbQuery
import com.example.models.user.Roles
import com.example.models.user.User
import org.postgresql.util.PSQLException
import java.sql.ResultSet
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

// move to exception package or something?
class RegistrationException(message: String, cause: Throwable? = null) : Exception(message, cause)

class LoginAlreadyTakenException(val login: String) : Exception("Login '$login' is already taken")

suspend fun registerUser(
    login: String,
    name: String,
    surname: String,
    password: String,
    role: Roles = Roles.USER,
): User {
    /*  raw sql request to create user.
        login: string with not null(and unique) string
        name: string with not null name string
        surname: string with not null surname string
        password: hashed with argon string, not null
        role: choice from Roles enum.

        return: created user OR RegistrationException.
    */
    return try {
        dbQuery { conn ->
            conn.prepareStatement("""
                INSERT INTO users (login, name, surname, password, role)
                VALUES (?, ?, ?, ?, ?)
                RETURNING id, login, name, surname, password, role, created_at, updated_at
            """.trimIndent()).use { ps ->
                ps.setString(1, login)
                ps.setString(2, name)
                ps.setString(3, surname)
                ps.setString(4, password)
                ps.setString(5, role.name)
                ps.executeQuery().use { rs ->
                    if (!rs.next()) error("INSERT returned no rows")
                    rs.toUser()
                }
            }
        }
    } catch (e: PSQLException) {
        // 23505 = unique_violation
        if (e.sqlState == "23505" && e.message?.contains("users_login_key") == true) {
            throw LoginAlreadyTakenException(login)
        }
        throw RegistrationException("Failed to register user, ${e.message}", e)
    }
}

class UserNotFoundException(val login: String) : Exception("User with login '$login' not found")

suspend fun getUserByLogin(login: String): User {
    val user = dbQuery { conn ->
        conn.prepareStatement("""
            SELECT id, login, name, surname, password, role, created_at, updated_at
            FROM users WHERE login = ?
        """.trimIndent()).use { ps ->
            ps.setString(1, login)
            ps.executeQuery().use { rs ->
                if (rs.next()) rs.toUser() else null
            }
        }
    }
    return user ?: throw UserNotFoundException(login)
}

@OptIn(ExperimentalTime::class)
private fun ResultSet.toUser(): User = User(
    id        = getLong("id"),
    login = getString("login"),
    name      = getString("name"),
    surname   = getString("surname"),
    password  = getString("password"),
    role      = Roles.valueOf(getString("role")),
    createdAt = Clock.System.now(),
    updatedAt = Clock.System.now(),  // maybe this needs to be null because we only created object?
)
