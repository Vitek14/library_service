package com.example.model.dto.rent

import kotlinx.serialization.Serializable

@Serializable
data class ApproveRentRequest(val expiresAt: String)  // will be in iso 8601 format
