package com.example

import com.auth0.jwt.JWT
import com.example.models.user.Roles
import com.example.models.user.User
import com.example.utilities.auth.generateJWT
import kotlin.test.*
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class JWTServiceTest {

    private fun makeUser(id: Long = 7) = User(
        id = id,
        login = "testuser",
        name = "Test",
        surname = "User",
        password = "hashed",
        role = Roles.USER,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now(),
    )

    @Test
    fun `generateJWT embeds user id claim`() {
        val issued = generateJWT(makeUser(id = 42))
        val decoded = JWT.decode(issued.token)
        assertEquals(42L, decoded.getClaim("id").asLong())
    }

    @Test
    fun `generateJWT sets issuer and audience`() {
        val decoded = JWT.decode(generateJWT(makeUser()).token)
        assertEquals("https://jwt-provider-domain/", decoded.issuer)
        assertTrue(decoded.audience.contains("jwt-audience"))
    }

    @Test
    fun `generateJWT sets future expiresAt`() {
        val issued = generateJWT(makeUser())
        assertTrue(issued.expiresAt > System.currentTimeMillis())
        assertTrue(JWT.decode(issued.token).expiresAt.time > System.currentTimeMillis())
    }
}
