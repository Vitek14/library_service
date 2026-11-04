package com.example.db.repository

import com.example.db.DatabaseFactory.withTransaction
import com.example.model.entity.Author
import java.sql.Connection
import java.sql.ResultSet

class AuthorRepository {

    suspend fun findById(id: Long): Author? = withTransaction { conn ->
        conn.prepareStatement(SQL_FIND_BY_ID).use { stmt ->
            stmt.setLong(1, id)
            stmt.executeQuery().use { rs -> if (rs.next()) rs.toAuthor() else null }
        }
    }

    suspend fun findAll(): List<Author> = withTransaction { conn ->
        conn.prepareStatement(SQL_FIND_ALL).use { stmt ->
            stmt.executeQuery().use { rs -> buildList { while (rs.next()) add(rs.toAuthor()) } }
        }
    }

    fun findOrCreateId(conn: Connection, name: String): Long {
        conn.prepareStatement(SQL_FIND_OR_CREATE).use { stmt ->
            stmt.setString(1, name.trim())
            stmt.executeQuery().use { rs ->
                check(rs.next()) { "INSERT ... ON CONFLICT ... RETURNING returned no row" }
                return rs.getLong(1)
            }
        }
    }

    companion object {
        private const val SQL_FIND_BY_ID = "SELECT id, name FROM authors WHERE id = ?"
        private const val SQL_FIND_ALL   = "SELECT id, name FROM authors ORDER BY name"

        private const val SQL_FIND_OR_CREATE =
            "INSERT INTO authors(name) VALUES (?) " +
            "ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name " +
            "RETURNING id"
    }
}

private fun ResultSet.toAuthor() = Author(id = getLong("id"), name = getString("name"))
