package com.example.model.dto.author

import kotlinx.serialization.Serializable

@Serializable
data class AuthorResponse(
    val id: Long,
    val name: String
)
