package com.example.security

import com.example.model.enums.Role

data class JwtPrincipal(
    val userId: Long,
    val login: String,
    val role: Role,
)
