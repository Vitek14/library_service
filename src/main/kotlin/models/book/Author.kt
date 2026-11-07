package com.example.models.book

import kotlinx.serialization.Serializable

@Serializable
data class Author(
    val id: Long,
    val name: String,
)
