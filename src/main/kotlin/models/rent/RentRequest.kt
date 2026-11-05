package com.example.models.rent

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class RentRequest @OptIn(ExperimentalTime::class) constructor(
    val id: Long,
    val bookId: Long,
    val userId: Long,
    val status: Status,
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now(),
    val expiresAt: Instant?
)
