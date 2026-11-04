package com.example.service

import com.example.db.repository.UserRepository
import com.example.model.dto.auth.AuthResponse
import com.example.model.dto.auth.LoginRequest
import com.example.model.dto.auth.RegisterRequest
import com.example.model.enums.Role
import com.example.security.JwtService
import com.example.security.PasswordHasher
import io.ktor.http.HttpStatusCode

class AuthException(status: HttpStatusCode, message: String) : ApiException(status, message)

class AuthService(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
    private val jwtService: JwtService,
) {

    suspend fun register(req: RegisterRequest): AuthResponse {
        if (req.login.isBlank() || req.password.isBlank()) {
            throw AuthException(HttpStatusCode.BadRequest, "login and password must not be blank")
        }
        if (userRepository.existsByLogin(req.login)) {
            throw AuthException(HttpStatusCode.Conflict, "login '${req.login}' is already taken")
        }
        val user = userRepository.create(
            name = req.name,
            surname = req.surname,
            login = req.login,
            passwordHash = passwordHasher.hash(req.password),
            role = Role.USER,
        )
        return AuthResponse(
            accessToken = jwtService.generateToken(user),
            expiresIn = jwtService.expiresInSeconds(),
        )
    }

    suspend fun login(req: LoginRequest): AuthResponse {
        val user = userRepository.findByLogin(req.login)
            ?: throw AuthException(HttpStatusCode.Unauthorized, "Invalid login or password")
        if (!passwordHasher.verify(req.password, user.passwordHash)) {
            throw AuthException(HttpStatusCode.Unauthorized, "Invalid login or password")
        }
        return AuthResponse(
            accessToken = jwtService.generateToken(user),
            expiresIn = jwtService.expiresInSeconds(),
        )
    }

    // creating adming by default if no found
    suspend fun bootstrapAdmin(login: String, password: String, name: String, surname: String) {
        if (userRepository.existsByRole(Role.ADMIN)) return
        userRepository.create(
            name = name,
            surname = surname,
            login = login,
            passwordHash = passwordHasher.hash(password),
            role = Role.ADMIN,
        )
    }
}
