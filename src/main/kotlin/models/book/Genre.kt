package com.example.models.book

import kotlinx.serialization.Serializable

@Serializable
data class Genre(
    val id: Long,
    val name: String,
)
