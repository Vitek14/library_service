package com.example.model.entity

import com.example.model.enums.RentRequestStatus
import java.time.Instant

data class RentRequest(
    val id: Long,
    val bookId: Long,
    val userId: Long,
    val status: RentRequestStatus,
    val requestedAt: Instant,
    val approvedAt: Instant?,
    val declinedAt: Instant?,
    val expiresAt: Instant?,
)
