package com.example.model.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val name: String,
    val surname: String,
    val login: String,
    val password: String,
)
