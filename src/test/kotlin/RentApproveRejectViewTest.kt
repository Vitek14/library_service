package com.example

import com.example.db.services.getUserById
import com.example.db.services.setRentStatus
import com.example.models.rent.RentRequest
import com.example.models.rent.Status
import com.example.models.user.Roles
import com.example.testUser
import com.example.utilities.auth.configureSecurity
import com.example.utilities.auth.generateJWT
import com.example.views.rent.RentApproveView
import com.example.views.rent.RentRejectView
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
import kotlin.time.ExperimentalTime
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate

class RentApproveRejectViewTest {

    private fun ApplicationTestBuilder.setup() {
        application {
            install(ContentNegotiation) { json() }
            configureSecurity()
            routing {
                authenticate("auth-jwt") {
                    RentApproveView()
                    RentRejectView()
                }
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun pending(id: Long) = RentRequest(
        id = id, bookId = 5, userId = 1, status = Status.PENDING,
        createdAt = Clock.System.now(), updatedAt = Clock.System.now(), expiresAt = null,
    )

    @Test
    fun `non-admin cannot approve`() {
        mockkStatic("com.example.db.services.UserServiceKt")
        try {
            coEvery { getUserById(1) } returns testUser(id = 1, role = Roles.USER)
            val token = generateJWT(testUser(id = 1, role = Roles.USER)).token

            testApplication {
                setup()
                val resp = client.post("/rent-requests/approve/7") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
                assertEquals(HttpStatusCode.Forbidden, resp.status)
            }
        } finally { unmockkAll() }
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `admin approves request`() {
        mockkStatic("com.example.db.services.UserServiceKt")
        mockkStatic("com.example.db.services.RentServiceKt")
        try {
            coEvery { getUserById(2) } returns testUser(id = 2, role = Roles.ADMIN)
            coEvery { setRentStatus(7L, Status.ACCEPTED) } returns pending(7).copy(status = Status.ACCEPTED)
            val token = generateJWT(testUser(id = 2, role = Roles.ADMIN)).token

            testApplication {
                setup()
                val resp = client.post("/rent-requests/approve/7") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
                assertEquals(HttpStatusCode.OK, resp.status)
                assertTrue(resp.bodyAsText().contains("ACCEPTED"))
            }
        } finally { unmockkAll() }
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `admin rejects request`() {
        mockkStatic("com.example.db.services.UserServiceKt")
        mockkStatic("com.example.db.services.RentServiceKt")
        try {
            coEvery { getUserById(2) } returns testUser(id = 2, role = Roles.ADMIN)
            coEvery { setRentStatus(8L, Status.REJECTED) } returns pending(8).copy(status = Status.REJECTED)
            val token = generateJWT(testUser(id = 2, role = Roles.ADMIN)).token

            testApplication {
                setup()
                val resp = client.post("/rent-requests/reject/8") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
                assertEquals(HttpStatusCode.OK, resp.status)
                assertTrue(resp.bodyAsText().contains("REJECTED"))
            }
        } finally { unmockkAll() }
    }
}
