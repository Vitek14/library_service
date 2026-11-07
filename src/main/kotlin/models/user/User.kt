package com.example.models.user

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class User @OptIn(ExperimentalTime::class) constructor(
    val id: Long,
    val login: String,
    val name: String,
    val surname: String,
    val password: String,
    val role: Roles,
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now()
)
