package com.example.Serializers.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthException(
    val message: String,
    val code: Int? = null,
)