package com.example

import com.example.views.auth.RegisterView

import com.example.db.services.LoginAlreadyTakenException
import com.example.db.services.registerUser
import com.example.models.user.Roles
import com.example.testUser
import com.example.utilities.auth.configureSecurity
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.*
import kotlin.test.*
import io.ktor.server.application.install

class RegisterViewTest {

    private fun ApplicationTestBuilder.setup() {
        application {
            install(ContentNegotiation) { json() }
            configureSecurity()
            routing { RegisterView() }
        }
    }

    @Test
    fun `returns 400 if any required field is blank`() = testApplication {
        setup()
        val resp = client.post("/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"","name":"N","surname":"S","password":"p"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, resp.status)
    }

    @Test
    fun `creates user and returns token`() {
        mockkStatic("com.example.db.services.UserServiceKt")
        try {
            coEvery {
                registerUser(any(), any(), any(), any(), any())
            } returns testUser(id = 10, login = "newbie", role = Roles.USER)

            testApplication {
                setup()
                val resp = client.post("/register") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"login":"newbie","name":"New","surname":"Bie","password":"secret"}""")
                }
                assertEquals(HttpStatusCode.OK, resp.status)
                assertTrue(resp.bodyAsText().contains("accessToken"))
            }
        } finally {
            unmockkAll()
        }
    }

    @Test
    fun `duplicate login currently throws 500 (bug)`() {
        mockkStatic("com.example.db.services.UserServiceKt")
        try {
            coEvery {
                registerUser(any(), any(), any(), any(), any())
            } throws LoginAlreadyTakenException("newbie")

            testApplication {
                setup()
                val resp = client.post("/register") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"login":"newbie","name":"N","surname":"S","password":"p"}""")
                }
                assertEquals(HttpStatusCode.InternalServerError, resp.status)
            }
        } finally {
            unmockkAll()
        }
    }
}
