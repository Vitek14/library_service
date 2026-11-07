package com.example

import com.example.db.services.createRentRequest
import com.example.db.services.getUserById
import com.example.models.rent.RentRequest
import com.example.models.rent.Status
import com.example.testUser
import com.example.utilities.auth.configureSecurity
import com.example.utilities.auth.generateJWT
import com.example.views.rent.RentBookView
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.*
import kotlin.test.*
import kotlin.time.Clock
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import kotlin.time.ExperimentalTime

class RentBookViewTest {

    private fun ApplicationTestBuilder.setup() {
        application {
            install(ContentNegotiation) { json() }
            configureSecurity()
            routing {
                authenticate("auth-jwt") {
                    RentBookView()
                }
            }
        }
    }

    @Test
    fun `returns 401 without token`() = testApplication {
        setup()
        val resp = client.post("/rent-requests/5")
        assertEquals(HttpStatusCode.Unauthorized, resp.status)
    }

    @Test
    fun `returns 400 for non-numeric id`() {
        val token = generateJWT(testUser(id = 1)).token
        testApplication {
            setup()
            val resp = client.post("/rent-requests/abc") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            assertEquals(HttpStatusCode.BadRequest, resp.status)
        }
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `creates a rent request`() {
        mockkStatic("com.example.db.services.UserServiceKt")
        mockkStatic("com.example.db.services.RentServiceKt")
        try {
            val user = testUser(id = 1)
            coEvery { getUserById(1) } returns user
            coEvery { createRentRequest(5L, 1L, any()) } returns RentRequest(
                id = 11, bookId = 5, userId = 1, status = Status.PENDING,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
                expiresAt = null,
            )
            val token = generateJWT(user).token

            testApplication {
                setup()
                val resp = client.post("/rent-requests/5") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
                assertEquals(HttpStatusCode.OK, resp.status)
                assertTrue(resp.bodyAsText().contains("PENDING"))
            }
        } finally { unmockkAll() }
    }
}
