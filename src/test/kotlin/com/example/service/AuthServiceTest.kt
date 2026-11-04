package com.example.service

import com.example.db.repository.UserRepository
import com.example.model.dto.auth.LoginRequest
import com.example.model.dto.auth.RegisterRequest
import com.example.model.entity.User
import com.example.model.enums.Role
import com.example.security.JwtService
import com.example.security.PasswordHasher
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import java.time.Instant
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AuthServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var passwordHasher: PasswordHasher
    private lateinit var jwtService: JwtService
    private lateinit var authService: AuthService

    @BeforeTest
    fun setup() {
        userRepository = mockk()
        passwordHasher = mockk()
        jwtService = mockk()
        authService = AuthService(userRepository, passwordHasher, jwtService)
        every { jwtService.generateToken(any()) } returns "mock-token"
        every { jwtService.expiresInSeconds() } returns 3600L
    }

    @Test
    fun `register creates user and returns token`() = runBlocking {
        coEvery { userRepository.existsByLogin("ivan") } returns false
        every { passwordHasher.hash("secret123") } returns "hashed"
        coEvery { userRepository.create(any(), any(), any(), any(), any()) } returns user(role = Role.USER)

        val result = authService.register(RegisterRequest("Ivan", "Ivanov", "ivan", "secret123"))

        assertEquals("mock-token", result.accessToken)
        assertEquals(3600L, result.expiresIn)
        coVerify { userRepository.create("Ivan", "Ivanov", "ivan", "hashed", Role.USER) }
    }

    @Test
    fun `register fails on duplicate login`() = runBlocking {
        coEvery { userRepository.existsByLogin("ivan") } returns true

        val ex = assertFailsWith<ApiException> {
            authService.register(RegisterRequest("Ivan", "Ivanov", "ivan", "secret123"))
        }
        assertEquals(HttpStatusCode.Conflict, ex.status)
    }

    @Test
    fun `register fails on blank login`() = runBlocking {
        val ex = assertFailsWith<ApiException> {
            authService.register(RegisterRequest("Ivan", "Ivanov", "", "secret123"))
        }
        assertEquals(HttpStatusCode.BadRequest, ex.status)
    }

    @Test
    fun `login succeeds with correct password`() = runBlocking {
        coEvery { userRepository.findByLogin("ivan") } returns user(passwordHash = "hashed")
        every { passwordHasher.verify("secret123", "hashed") } returns true

        val result = authService.login(LoginRequest("ivan", "secret123"))

        assertEquals("mock-token", result.accessToken)
    }

    @Test
    fun `login fails on wrong password`() = runBlocking {
        coEvery { userRepository.findByLogin("ivan") } returns user(passwordHash = "hashed")
        every { passwordHasher.verify("wrong", "hashed") } returns false

        val ex = assertFailsWith<ApiException> {
            authService.login(LoginRequest("ivan", "wrong"))
        }
        assertEquals(HttpStatusCode.Unauthorized, ex.status)
    }

    @Test
    fun `login fails on unknown user`() = runBlocking {
        coEvery { userRepository.findByLogin("nobody") } returns null

        val ex = assertFailsWith<ApiException> {
            authService.login(LoginRequest("nobody", "x"))
        }
        assertEquals(HttpStatusCode.Unauthorized, ex.status)
    }

    @Test
    fun `bootstrapAdmin creates admin when none exists`() = runBlocking {
        coEvery { userRepository.existsByRole(Role.ADMIN) } returns false
        coEvery { userRepository.create(any(), any(), any(), any(), Role.ADMIN) } returns user(role = Role.ADMIN)
        every { passwordHasher.hash("admin-pass") } returns "hashed-admin"

        authService.bootstrapAdmin("admin", "admin-pass", "Admin", "Adminov")

        coVerify { userRepository.create("Admin", "Adminov", "admin", "hashed-admin", Role.ADMIN) }
    }

    @Test
    fun `bootstrapAdmin does nothing when admin exists`() = runBlocking {
        coEvery { userRepository.existsByRole(Role.ADMIN) } returns true

        authService.bootstrapAdmin("admin", "admin-pass", "Admin", "Adminov")

        coVerify(exactly = 0) { userRepository.create(any(), any(), any(), any(), any()) }
    }

    private fun user(role: Role = Role.USER, passwordHash: String = "hashed") = User(
        id = 1L,
        name = "Ivan",
        surname = "Ivanov",
        login = "ivan",
        passwordHash = passwordHash,
        role = role,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )
}
