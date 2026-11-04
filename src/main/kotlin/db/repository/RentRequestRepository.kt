package com.example.db.repository

import com.example.db.DatabaseFactory.withTransaction
import com.example.model.entity.RentRequest
import com.example.model.enums.RentRequestStatus
import java.sql.ResultSet
import java.time.Instant

class RentRequestRepository {

    suspend fun findById(id: Long): RentRequest? = withTransaction { conn ->
        conn.prepareStatement(SQL_FIND_BY_ID).use { stmt ->
            stmt.setLong(1, id)
            stmt.executeQuery().use { rs -> if (rs.next()) rs.toRentRequest() else null }
        }
    }

    // pending or approved request from user
    suspend fun findActiveByBookAndUser(bookId: Long, userId: Long): RentRequest? =
        withTransaction { conn ->
            conn.prepareStatement(SQL_FIND_ACTIVE).use { stmt ->
                stmt.setLong(1, bookId)
                stmt.setLong(2, userId)
                stmt.executeQuery().use { rs -> if (rs.next()) rs.toRentRequest() else null }
            }
        }

    suspend fun existsDeclinedByBookAndUser(bookId: Long, userId: Long): Boolean =
        withTransaction { conn ->
            conn.prepareStatement(SQL_EXISTS_DECLINED).use { stmt ->
                stmt.setLong(1, bookId)
                stmt.setLong(2, userId)
                stmt.executeQuery().use { rs -> rs.next() && rs.getBoolean(1) }
            }
        }

    suspend fun findApprovedByBook(bookId: Long): RentRequest? = withTransaction { conn ->
        conn.prepareStatement(SQL_FIND_APPROVED_FOR_BOOK).use { stmt ->
            stmt.setLong(1, bookId)
            stmt.executeQuery().use { rs -> if (rs.next()) rs.toRentRequest() else null }
        }
    }

    suspend fun findAllPending(): List<RentRequest> = withTransaction { conn ->
        conn.prepareStatement(SQL_FIND_ALL_PENDING).use { stmt ->
            stmt.executeQuery().use { rs ->
                buildList { while (rs.next()) add(rs.toRentRequest()) }
            }
        }
    }

    /*
    creates pending request.
    will fail if our unique index fails(when user trying to request declined by admin book)
     */
    suspend fun create(bookId: Long, userId: Long): RentRequest = withTransaction { conn ->
        conn.prepareStatement(
            "INSERT INTO rent_requests(book_id, user_id, status) " +
            "VALUES (?, ?, 'PENDING') RETURNING id, book_id, user_id, status, " +
            "requested_at, approved_at, declined_at, expires_at"
        ).use { stmt ->
            stmt.setLong(1, bookId)
            stmt.setLong(2, userId)
            stmt.executeQuery().use { rs ->
                check(rs.next()) { "INSERT INTO rent_requests RETURNING returned no row" }
                rs.toRentRequest()
            }
        }
    }

    // true if success, false if not pending or does not exist
    suspend fun approve(id: Long, expiresAt: Instant): Boolean = withTransaction { conn ->
        conn.prepareStatement(
            "UPDATE rent_requests SET status='APPROVED', approved_at=now(), expires_at=? " +
            "WHERE id=? AND status='PENDING' RETURNING id"
        ).use { stmt ->
            stmt.setTimestamp(1, java.sql.Timestamp.from(expiresAt))
            stmt.setLong(2, id)
            stmt.executeQuery().use { rs -> rs.next() }
        }
    }

    suspend fun decline(id: Long): Boolean = withTransaction { conn ->
        conn.prepareStatement(
            "UPDATE rent_requests SET status='DECLINED', declined_at=now() " +
            "WHERE id=? AND status='PENDING' RETURNING id"
        ).use { stmt ->
            stmt.setLong(1, id)
            stmt.executeQuery().use { rs -> rs.next() }
        }
    }

    companion object {
        private const val SELECT_COLUMNS =
            "id, book_id, user_id, status, requested_at, approved_at, declined_at, expires_at"

        private const val SQL_FIND_BY_ID =
            "SELECT $SELECT_COLUMNS FROM rent_requests WHERE id = ?"

        private const val SQL_FIND_ACTIVE =
            "SELECT $SELECT_COLUMNS FROM rent_requests " +
            "WHERE book_id = ? AND user_id = ? AND status IN ('PENDING', 'APPROVED')"

        private const val SQL_EXISTS_DECLINED =
            "SELECT EXISTS(SELECT 1 FROM rent_requests " +
            "WHERE book_id = ? AND user_id = ? AND status = 'DECLINED')"

        private const val SQL_FIND_APPROVED_FOR_BOOK =
            "SELECT $SELECT_COLUMNS FROM rent_requests " +
            "WHERE book_id = ? AND status = 'APPROVED'"

        private const val SQL_FIND_ALL_PENDING =
            "SELECT $SELECT_COLUMNS FROM rent_requests WHERE status = 'PENDING' " +
            "ORDER BY requested_at ASC"
    }
}

private fun ResultSet.toRentRequest() = RentRequest(
    id = getLong("id"),
    bookId = getLong("book_id"),
    userId = getLong("user_id"),
    status = RentRequestStatus.valueOf(getString("status")),
    requestedAt = getTimestamp("requested_at").toInstant(),
    approvedAt = getTimestamp("approved_at")?.toInstant(),
    declinedAt = getTimestamp("declined_at")?.toInstant(),
    expiresAt  = getTimestamp("expires_at")?.toInstant(),
)
