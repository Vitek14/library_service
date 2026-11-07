package com.example.models.rent

import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
data class RentRequest @OptIn(ExperimentalTime::class) constructor(
    val id: Long,
    val bookId: Long,
    val userId: Long,
    val status: Status,
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now(),
    val expiresAt: Instant?
)
