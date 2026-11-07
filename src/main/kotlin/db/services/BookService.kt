package com.example.db.services

import com.example.db.dbQuery
import com.example.models.book.Author
import com.example.models.book.Book
import com.example.models.book.Genre
import org.postgresql.util.PSQLException
import java.sql.Connection
import kotlin.time.ExperimentalTime

class BookNotFoundException(val id: Long) : Exception("Book with id $id not found")
class BookCreationException(message: String, cause: Throwable? = null) : Exception(message, cause)

// list(get request)
@OptIn(ExperimentalTime::class)
suspend fun getBooks(): List<Book> = dbQuery { conn ->
    conn.prepareStatement("SELECT id, title FROM books ORDER BY id").use { ps ->
        ps.executeQuery().use { rs ->
            val books = mutableListOf<Book>()
            while (rs.next()) {
                val id = rs.getLong("id")
                books.add(
                    Book(
                        id = id,
                        title = rs.getString("title"),
                        authors = fetchAuthors(conn, id),
                        genres = fetchGenres(conn, id),
                    )
                )
            }
            books
        }
    }
}

suspend fun createAuthor(name: String): Author = dbQuery { conn ->
    conn.prepareStatement("INSERT INTO authors (name) VALUES (?) RETURNING id, name").use { ps ->
        ps.setString(1, name)
        ps.executeQuery().use { rs ->
            if (!rs.next()) error("INSERT returned no rows")
            Author(id = rs.getLong("id"), name = rs.getString("name"))
        }
    }
}

suspend fun createGenre(name: String): Genre = dbQuery { conn ->
    conn.prepareStatement("INSERT INTO genres (name) VALUES (?) RETURNING id, name").use { ps ->
        ps.setString(1, name)
        ps.executeQuery().use { rs ->
            if (!rs.next()) error("INSERT returned no rows")
            Genre(id = rs.getLong("id"), name = rs.getString("name"))
        }
    }
}

@OptIn(ExperimentalTime::class)
suspend fun createBook(title: String, authorIds: List<Long>, genreIds: List<Long>): Book {
    return try {
        dbQuery { conn ->
            val bookId = conn.prepareStatement(
                "INSERT INTO books (title) VALUES (?) RETURNING id"
            ).use { ps ->
                ps.setString(1, title)
                ps.executeQuery().use { rs ->
                    if (!rs.next()) error("INSERT returned no rows")
                    rs.getLong("id")
                }
            }

            if (authorIds.isNotEmpty()) {
                conn.prepareStatement(
                    "INSERT INTO book_authors (book_id, author_id) VALUES (?, ?)"
                ).use { ps ->
                    for (authorId in authorIds) {
                        ps.setLong(1, bookId)
                        ps.setLong(2, authorId)
                        ps.addBatch()
                    }
                    ps.executeBatch()
                }
            }

            if (genreIds.isNotEmpty()) {
                conn.prepareStatement(
                    "INSERT INTO book_genres (book_id, genre_id) VALUES (?, ?)"
                ).use { ps ->
                    for (genreId in genreIds) {
                        ps.setLong(1, bookId)
                        ps.setLong(2, genreId)
                        ps.addBatch()
                    }
                    ps.executeBatch()
                }
            }

            Book(
                id      = bookId,
                title   = title,
                authors = fetchAuthors(conn, bookId),
                genres  = fetchGenres(conn, bookId),
            )
        }
    } catch (e: PSQLException) {
        // 23503 = foreign_key_violation
        throw BookCreationException("Failed to create book, ${e.message}", e)
    }
}

private fun fetchAuthors(conn: Connection, bookId: Long): List<Author> {
    return conn.prepareStatement("""
        SELECT a.id, a.name
        FROM authors a
        JOIN book_authors ba ON ba.author_id = a.id
        WHERE ba.book_id = ?
        ORDER BY a.id
    """.trimIndent()).use { ps ->
        ps.setLong(1, bookId)
        ps.executeQuery().use { rs ->
            val list = mutableListOf<Author>()
            while (rs.next()) list.add(Author(id = rs.getLong("id"), name = rs.getString("name")))
            list
        }
    }
}

private fun fetchGenres(conn: Connection, bookId: Long): List<Genre> {
    return conn.prepareStatement("""
        SELECT g.id, g.name
        FROM genres g
        JOIN book_genres bg ON bg.genre_id = g.id
        WHERE bg.book_id = ?
        ORDER BY g.id
    """.trimIndent()).use { ps ->
        ps.setLong(1, bookId)
        ps.executeQuery().use { rs ->
            val list = mutableListOf<Genre>()
            while (rs.next()) list.add(Genre(id = rs.getLong("id"), name = rs.getString("name")))
            list
        }
    }
}

