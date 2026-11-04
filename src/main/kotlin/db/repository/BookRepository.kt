package com.example.db.repository

import com.example.db.DatabaseFactory.withTransaction
import com.example.model.entity.Author
import com.example.model.entity.Book
import com.example.model.entity.Genre
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement

class BookRepository(
    private val authorRepository: AuthorRepository,
    private val genreRepository: GenreRepository,
) {

    suspend fun findById(id: Long): Book? = findByIds(listOf(id)).firstOrNull()

    suspend fun existsById(id: Long): Boolean = withTransaction { conn ->
        conn.prepareStatement("SELECT EXISTS(SELECT 1 FROM books WHERE id = ?)").use { stmt ->
            stmt.setLong(1, id)
            stmt.executeQuery().use { rs -> rs.next() && rs.getBoolean(1) }
        }
    }

    suspend fun findByIds(ids: Collection<Long>): List<Book> {
        if (ids.isEmpty()) return emptyList()
        return withTransaction { conn ->
            val books = loadBooks(conn, ids)
            if (books.isEmpty()) return@withTransaction emptyList()
            val bookIds = books.map { it.id }
            val authorsByBook = loadAuthorsForBooks(conn, bookIds)
            val genresByBook  = loadGenresForBooks(conn, bookIds)
            books.map {
                it.copy(
                    authors = authorsByBook[it.id].orEmpty(),
                    genres  = genresByBook[it.id].orEmpty(),
                )
            }
        }
    }

    // not required, but for better interface
    suspend fun findAll(): List<Book> = withTransaction { conn ->
        conn.prepareStatement("SELECT id, title, created_at, updated_at FROM books ORDER BY title")
            .use { stmt ->
                stmt.executeQuery().use { rs ->
                    val books = buildList {
                        while (rs.next()) add(rs.toBook(authors = emptyList(), genres = emptyList()))
                    }
                    if (books.isEmpty()) return@withTransaction emptyList()
                    val bookIds = books.map { it.id }
                    val authorsByBook = loadAuthorsForBooks(conn, bookIds)
                    val genresByBook  = loadGenresForBooks(conn, bookIds)
                    books.map {
                        it.copy(
                            authors = authorsByBook[it.id].orEmpty(),
                            genres  = genresByBook[it.id].orEmpty(),
                        )
                    }
                }
            }
    }

    // creates book, resolves author,genres via upsert
    suspend fun create(
        title: String,
        authorNames: List<String>,
        genreNames: List<String>,
    ): Book = withTransaction { conn ->
        val authorIds = authorNames.map { authorRepository.findOrCreateId(conn, it) }.distinct()
        val genreIds  = genreNames.map  { genreRepository.findOrCreateId(conn, it) }.distinct()

        val bookId = conn.prepareStatement(
            "INSERT INTO books(title) VALUES (?) RETURNING id",
            Statement.RETURN_GENERATED_KEYS,
        ).use { stmt ->
            stmt.setString(1, title.trim())
            stmt.executeQuery().use { rs ->
                check(rs.next()) { "INSERT INTO books RETURNING id returned no row" }
                rs.getLong(1)
            }
        }

        insertLinks(conn, "book_authors", "author_id", bookId, authorIds)
        insertLinks(conn, "book_genres",  "genre_id",  bookId, genreIds)

        findByIdInConnection(conn, bookId)
            ?: error("Book not found after insert, should never happen")
    }

    // helpers

    private fun loadBooks(conn: Connection, ids: Collection<Long>): List<Book> =
        conn.prepareStatement(
            "SELECT id, title, created_at, updated_at FROM books WHERE id = ANY(?)"
        ).use { stmt ->
            stmt.setArray(1, conn.createArrayOf("bigint", ids.toTypedArray()))
            stmt.executeQuery().use { rs ->
                buildList { while (rs.next()) add(rs.toBook(authors = emptyList(), genres = emptyList())) }
            }
        }

    private fun loadAuthorsForBooks(conn: Connection, bookIds: List<Long>): Map<Long, List<Author>> =
        conn.prepareStatement(
            "SELECT ba.book_id, a.id, a.name FROM book_authors ba " +
            "JOIN authors a ON a.id = ba.author_id " +
            "WHERE ba.book_id = ANY(?) ORDER BY a.name"
        ).use { stmt ->
            stmt.setArray(1, conn.createArrayOf("bigint", bookIds.toTypedArray()))
            stmt.executeQuery().use { rs ->
                val map = mutableMapOf<Long, MutableList<Author>>()
                while (rs.next()) {
                    map.getOrPut(rs.getLong("book_id")) { mutableListOf() }
                       .add(Author(rs.getLong("id"), rs.getString("name")))
                }
                map
            }
        }

    private fun loadGenresForBooks(conn: Connection, bookIds: List<Long>): Map<Long, List<Genre>> =
        conn.prepareStatement(
            "SELECT bg.book_id, g.id, g.name FROM book_genres bg " +
            "JOIN genres g ON g.id = bg.genre_id " +
            "WHERE bg.book_id = ANY(?) ORDER BY g.name"
        ).use { stmt ->
            stmt.setArray(1, conn.createArrayOf("bigint", bookIds.toTypedArray()))
            stmt.executeQuery().use { rs ->
                val map = mutableMapOf<Long, MutableList<Genre>>()
                while (rs.next()) {
                    map.getOrPut(rs.getLong("book_id")) { mutableListOf() }
                       .add(Genre(rs.getLong("id"), rs.getString("name")))
                }
                map
            }
        }

    private fun insertLinks(
        conn: Connection,
        table: String,
        fkColumn: String,
        bookId: Long,
        linkedIds: List<Long>,
    ) {
        if (linkedIds.isEmpty()) return
        conn.prepareStatement(
            "INSERT INTO $table(book_id, $fkColumn) VALUES (?, ?)"
        ).use { stmt ->
            linkedIds.forEach { id ->
                stmt.setLong(1, bookId)
                stmt.setLong(2, id)
                stmt.addBatch()
            }
            stmt.executeBatch()
        }
    }

    private fun findByIdInConnection(conn: Connection, id: Long): Book? =
        findByIdsInConnection(conn, listOf(id)).firstOrNull()

    private fun findByIdsInConnection(conn: Connection, ids: List<Long>): List<Book> {
        if (ids.isEmpty()) return emptyList()
        val books = loadBooks(conn, ids)
        if (books.isEmpty()) return emptyList()
        val bookIds = books.map { it.id }
        val authorsByBook = loadAuthorsForBooks(conn, bookIds)
        val genresByBook  = loadGenresForBooks(conn, bookIds)
        return books.map {
            it.copy(authors = authorsByBook[it.id].orEmpty(), genres = genresByBook[it.id].orEmpty())
        }
    }
}

private fun ResultSet.toBook(authors: List<Author>, genres: List<Genre>) = Book(
    id = getLong("id"),
    title = getString("title"),
    authors = authors,
    genres = genres,
    createdAt = getTimestamp("created_at").toInstant(),
    updatedAt = getTimestamp("updated_at").toInstant(),
)
