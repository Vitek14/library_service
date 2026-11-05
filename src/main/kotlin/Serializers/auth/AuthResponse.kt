package com.example.Serializers.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    val accessToken: String,
    val expiresAt: Long,
)
