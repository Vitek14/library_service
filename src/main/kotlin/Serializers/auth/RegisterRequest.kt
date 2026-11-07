package com.example.Serializers.auth

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val login: String,
    val name: String,
    val surname: String,
    val password: String
)
