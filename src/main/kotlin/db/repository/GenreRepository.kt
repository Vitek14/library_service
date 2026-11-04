package com.example.db.repository

import com.example.db.DatabaseFactory.withTransaction
import com.example.model.entity.Genre
import java.sql.Connection
import java.sql.ResultSet

class GenreRepository {
        suspend fun findById(id: Long): Genre? = withTransaction { conn ->
        conn.prepareStatement(SQL_FIND_BY_ID).use { stmt ->
            stmt.setLong(1, id)
            stmt.executeQuery().use { rs -> if (rs.next()) rs.toGenre() else null }
        }
    }

    suspend fun findAll(): List<Genre> = withTransaction { conn ->
        conn.prepareStatement(SQL_FIND_ALL).use { stmt ->
            stmt.executeQuery().use { rs -> buildList { while (rs.next()) add(rs.toGenre()) } }
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
        private const val SQL_FIND_BY_ID = "SELECT id, name FROM genres WHERE id = ?"
        private const val SQL_FIND_ALL   = "SELECT id, name FROM genres ORDER BY name"
        private const val SQL_FIND_OR_CREATE =
            "INSERT INTO genres(name) VALUES (?) " +
            "ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name " +
            "RETURNING id"
    }
}

private fun ResultSet.toGenre() = Genre(id = getLong("id"), name = getString("name"))
