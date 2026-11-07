package com.example

import com.example.db.services.UserNotFoundException
import com.example.db.services.getUserByLogin
import com.example.testUser
import com.example.utilities.auth.PasswordHasher
import com.example.utilities.auth.configureSecurity
import com.example.views.auth.LoginView
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.*
import kotlin.test.*

class LoginViewTest {

    private fun ApplicationTestBuilder.setup() {
        application {
            install(ContentNegotiation) { json() }
            configureSecurity()
            routing { LoginView() }
        }
    }

    @Test
    fun `returns 400 when login is empty`() = testApplication {
        setup()
        val resp = client.post("/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"","password":"secret"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, resp.status)
    }

    @Test
    fun `returns 400 when password is empty`() = testApplication {
        setup()
        val resp = client.post("/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"testuser","password":""}""")
        }
        assertEquals(HttpStatusCode.BadRequest, resp.status)
    }

    @Test
    fun `returns token for valid credentials`() {
        mockkStatic("com.example.db.services.UserServiceKt")
        mockkObject(PasswordHasher)
        try {
            coEvery { getUserByLogin("testuser") } returns testUser()
            every { PasswordHasher.verifyPassword(any(), any()) } returns true

            testApplication {
                setup()
                val resp = client.post("/login") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"login":"testuser","password":"correct"}""")
                }
                assertEquals(HttpStatusCode.OK, resp.status)
                val body = resp.bodyAsText()
                assertTrue(body.contains("accessToken"))
                assertTrue(body.contains("expiresAt"))
            }
        } finally {
            unmockkAll()
        }
    }

    @Test
    fun `returns 400 for wrong password`() {
        mockkStatic("com.example.db.services.UserServiceKt")
        mockkObject(PasswordHasher)
        try {
            coEvery { getUserByLogin("testuser") } returns testUser()
            every { PasswordHasher.verifyPassword(any(), any()) } returns false

            testApplication {
                setup()
                val resp = client.post("/login") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"login":"testuser","password":"wrong"}""")
                }
                assertEquals(HttpStatusCode.BadRequest, resp.status)
            }
        } finally {
            unmockkAll()
        }
    }

    @Test
    fun `unknown login currently leaks as 500 (bug)`() {
        mockkStatic("com.example.db.services.UserServiceKt")
        try {
            coEvery { getUserByLogin("ghost") } throws UserNotFoundException("ghost")

            testApplication {
                setup()
                val resp = client.post("/login") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"login":"ghost","password":"x"}""")
                }
                assertEquals(HttpStatusCode.InternalServerError, resp.status)
            }
        } finally {
            unmockkAll()
        }
    }
}
