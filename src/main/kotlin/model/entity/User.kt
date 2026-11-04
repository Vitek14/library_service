package com.example.model.entity

import com.example.model.enums.Role
import java.time.Instant

data class User(
    val id: Long,
    val name: String,
    val surname: String,
    val login: String,
    val passwordHash: String,
    val role: Role,
    val createdAt: Instant,
    val updatedAt: Instant,
)
