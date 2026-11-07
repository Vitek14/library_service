package com.example

import com.example.db.services.createBook
import com.example.db.services.getUserById
import com.example.models.book.Author
import com.example.models.book.Book
import com.example.models.book.Genre
import com.example.models.user.Roles
import com.example.testUser
import com.example.utilities.auth.configureSecurity
import com.example.utilities.auth.generateJWT
import com.example.views.book.CreateBook
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
import io.ktor.server.auth.authenticate

class CreateBookViewTest {

    private fun ApplicationTestBuilder.setup() {
        application {
            install(ContentNegotiation) { json() }
            configureSecurity()
            routing {
                authenticate("auth-jwt") {
                    CreateBook()
                }
            }
        }
    }

    @Test
    fun `returns 401 without token`() = testApplication {
        setup()
        val resp = client.post("/books") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"X","authors":[1],"genres":[1]}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, resp.status)
    }

    @Test
    fun `returns 403 for non-admin user`() {
        mockkStatic("com.example.db.services.UserServiceKt")
        mockkStatic("com.example.db.services.BookServiceKt")
        try {
            coEvery { getUserById(1) } returns testUser(id = 1, role = Roles.USER)

            val token = generateJWT(testUser(id = 1, role = Roles.USER)).token

            testApplication {
                setup()
                val resp = client.post("/books") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody("""{"title":"X","authors":[1],"genres":[1]}""")
                }
                assertEquals(HttpStatusCode.Forbidden, resp.status)
            }
        } finally { unmockkAll() }
    }

    @Test
    fun `returns 400 on empty title`() {
        mockkStatic("com.example.db.services.UserServiceKt")
        try {
            coEvery { getUserById(2) } returns testUser(id = 2, role = Roles.ADMIN)

            val token = generateJWT(testUser(id = 2, role = Roles.ADMIN)).token

            testApplication {
                setup()
                val resp = client.post("/books") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody("""{"title":"","authors":[1],"genres":[1]}""")
                }
                assertEquals(HttpStatusCode.BadRequest, resp.status)
            }
        } finally { unmockkAll() }
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `admin creates a book`() {
        mockkStatic("com.example.db.services.UserServiceKt")
        mockkStatic("com.example.db.services.BookServiceKt")
        try {
            coEvery { getUserById(2) } returns testUser(id = 2, role = Roles.ADMIN)
            coEvery { createBook("Kotlin", listOf(1L), listOf(2L)) } returns
                Book(99, "Kotlin", listOf(Author(1, "A")), listOf(Genre(2, "G")))

            val token = generateJWT(testUser(id = 2, role = Roles.ADMIN)).token

            testApplication {
                setup()
                val resp = client.post("/books") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody("""{"title":"Kotlin","authors":[1],"genres":[2]}""")
                }
                assertEquals(HttpStatusCode.OK, resp.status)
                assertTrue(resp.bodyAsText().contains("Kotlin"))
            }
        } finally { unmockkAll() }
    }
}
