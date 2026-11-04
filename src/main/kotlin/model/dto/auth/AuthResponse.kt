package com.example.model.dto.auth

data class AuthResponse(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Int,
)
