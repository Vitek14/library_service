package com.example

import com.example.db.services.getBooks
import com.example.models.book.Author
import com.example.models.book.Book
import com.example.models.book.Genre
import com.example.views.book.ListBooksView
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.*
import kotlin.test.*
import kotlin.time.ExperimentalTime
import io.ktor.server.application.install

class ListBooksViewTest {

    @OptIn(ExperimentalTime::class)
    @Test
    fun `returns list of books`() {
        mockkStatic("com.example.db.services.BookServiceKt")
        try {
            coEvery { getBooks() } returns listOf(
                Book(1, "Kotlin in Action", listOf(Author(1, "Dmitry")), listOf(Genre(1, "IT"))),
                Book(2, "Clean Code",      emptyList(),                   emptyList()),
            )

            testApplication {
                application {
                    install(ContentNegotiation) { json() }
                    routing { ListBooksView() }
                }
                val resp = client.get("/books")
                assertEquals(HttpStatusCode.OK, resp.status)
                val body = resp.bodyAsText()
                assertTrue(body.contains("Kotlin in Action"))
                assertTrue(body.contains("Clean Code"))
            }
        } finally {
            unmockkAll()
        }
    }
}
