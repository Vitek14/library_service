package com.example.model.dto.genre

import kotlinx.serialization.Serializable

@Serializable
data class GenreResponse(
    val id: Long,
    val name: String
)
