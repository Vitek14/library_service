package com.example.db.services

import com.example.db.dbQuery
import com.example.models.rent.RentRequest
import com.example.models.rent.Status
import org.postgresql.util.PSQLException
import java.sql.ResultSet
import java.sql.Timestamp
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class RentRequestNotFoundException(val id: Long) : Exception("Rent request with id $id not found")
class RentRequestCreationException(message: String, cause: Throwable? = null) : Exception(message, cause)
class RentRequestAlreadyFinalizedException(val bookId: Long, val userId: Long) :
    Exception("User $userId already has a finalized rent request for book $bookId")
class InvalidRentStatusException(message: String) : Exception(message)

@OptIn(ExperimentalTime::class)
suspend fun createRentRequest(bookId: Long, userId: Long, expiresAt: Instant? = null): RentRequest {
    return try {
        dbQuery { conn ->
            // app-level check: no new request if a finalized one exists for this (book, user)
            val hasFinalized = conn.prepareStatement("""
                SELECT 1 FROM rent_requests
                WHERE book_id = ? AND user_id = ? AND status IN ('ACCEPTED', 'REJECTED')
                LIMIT 1
            """.trimIndent()).use { ps ->
                ps.setLong(1, bookId)
                ps.setLong(2, userId)
                ps.executeQuery().use { it.next() }
            }
            if (hasFinalized) throw RentRequestAlreadyFinalizedException(bookId, userId)

            val sql = if (expiresAt != null)
                "INSERT INTO rent_requests (book_id, user_id, expires_at) VALUES (?, ?, ?) RETURNING *"
            else
                "INSERT INTO rent_requests (book_id, user_id) VALUES (?, ?) RETURNING *"

            conn.prepareStatement(sql).use { ps ->
                ps.setLong(1, bookId)
                ps.setLong(2, userId)
                if (expiresAt != null) ps.setTimestamp(3, Timestamp(expiresAt.toEpochMilliseconds()))
                ps.executeQuery().use { rs ->
                    if (!rs.next()) error("INSERT returned no rows")
                    rs.toRentRequest()
                }
            }
        }
    } catch (e: PSQLException) {
        // 23503 = foreign_key_violation (book or user not found)
        throw RentRequestCreationException("Failed to create rent request, ${e.message}", e)
    }
}

suspend fun getAllRentRequests(): List<RentRequest> = dbQuery { conn ->
    conn.prepareStatement("SELECT * FROM rent_requests ORDER BY id").use { ps ->
        ps.executeQuery().use { rs ->
            val list = mutableListOf<RentRequest>()
            while (rs.next()) list.add(rs.toRentRequest())
            list
        }
    }
}

suspend fun setRentStatus(id: Long, status: Status): RentRequest {
    if (status == Status.PENDING) throw InvalidRentStatusException("Cannot set status to PENDING")

    return dbQuery { conn ->
        val current = conn.prepareStatement("SELECT status FROM rent_requests WHERE id = ?").use { ps ->
            ps.setLong(1, id)
            ps.executeQuery().use { rs -> if (rs.next()) rs.getString("status") else null }
        } ?: throw RentRequestNotFoundException(id)

        if (current != "PENDING")
            throw InvalidRentStatusException("Rent request $id is already $current")

        conn.prepareStatement("""
            UPDATE rent_requests
            SET status = ?, updated_at = NOW()
            WHERE id = ?
            RETURNING *
        """.trimIndent()).use { ps ->
            ps.setString(1, status.name)
            ps.setLong(2, id)
            ps.executeQuery().use { rs ->
                if (!rs.next()) error("UPDATE returned no rows")
                rs.toRentRequest()
            }
        }
    }
}


@OptIn(ExperimentalTime::class)
private fun ResultSet.toRentRequest(): RentRequest = RentRequest(
    id        = getLong("id"),
    bookId    = getLong("book_id"),
    userId    = getLong("user_id"),
    status    = Status.valueOf(getString("status")),
    createdAt = getTimestamp("created_at").toKotlinInstant(),
    updatedAt = getTimestamp("updated_at").toKotlinInstant(),
    expiresAt = getTimestamp("expires_at")?.toKotlinInstant(),
)

@OptIn(ExperimentalTime::class)
private fun Timestamp.toKotlinInstant(): Instant {
    val ji = toInstant()
    return Instant.fromEpochSeconds(ji.epochSecond, ji.nano.toLong())
}
