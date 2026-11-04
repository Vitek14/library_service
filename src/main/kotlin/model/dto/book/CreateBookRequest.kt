package com.example.model.dto.book

import kotlinx.serialization.Serializable

@Serializable
data class CreateBookRequest(
    val title: String,
    val authors: List<String>,
    val genres: List<String>,
)
