package com.example.db.services

import com.example.db.dbQuery
import com.example.models.user.Roles
import com.example.models.user.User
import java.sql.ResultSet
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

// move to exception package or something?
class RegistrationException(message: String, cause: Throwable? = null) : Exception(message, cause)

suspend fun registerUser(
    name: String,
    surname: String,
    password: String,
    role: Roles = Roles.USER,
): User {
    /*  raw sql request to create user.
        name: string with not null name string
        surname: string with not null surname string
        password: hashed with argon string, not null
        role: choice from Roles enum.

        return: created user OR RegistrationException.
    */
    return try {
        dbQuery { conn ->
            conn.prepareStatement("""
                INSERT INTO users (name, surname, password, role)
                VALUES (?, ?, ?, ?)
                RETURNING id, name, surname, password, role, created_at, updated_at
            """.trimIndent()).use { ps ->
                ps.setString(1, name)
                ps.setString(2, surname)
                ps.setString(3, password)
                ps.setString(4, role.name)
                ps.executeQuery().use { rs ->
                    if (!rs.next()) error("INSERT returned no rows")
                    rs.toUser()
                }
            }
        }
    } catch (e: Exception) {
        throw RegistrationException("Failed to register user, ${e.message}", e)
    }
}

@OptIn(ExperimentalTime::class)
private fun ResultSet.toUser(): User = User(
    id        = getLong("id"),
    name      = getString("name"),
    surname   = getString("surname"),
    password  = getString("password"),
    role      = Roles.valueOf(getString("role")),
    createdAt = Clock.System.now(),
    updatedAt = Clock.System.now(),  // maybe this needs to be null because we only created object?
)
