package com.example.model.dto.rent

import kotlinx.serialization.Serializable

@Serializable
data class CreateRentRequest(val bookId: Long)
