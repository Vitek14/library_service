package com.example.model.dto.book

import kotlinx.serialization.Serializable

@Serializable
data class BookResponse(
    val id: Long,
    val title: String,
    val authors: List<String>,
    val genres: List<String>,
    val createdAt: String,
    val updatedAt: String,
)
