package com.example.model.dto.rent

import kotlinx.serialization.Serializable

@Serializable
data class RentRequestResponse(
    val id: Long,
    val bookId: Long,
    val bookTitle: String,
    val userId: Long,
    val userLogin: String,
    val status: String,
    val requestedAt: String,
    val approvedAt: String?,
    val declinedAt: String?,
    val expiresAt: String?,
)
