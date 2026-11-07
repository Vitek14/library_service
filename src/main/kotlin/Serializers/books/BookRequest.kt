package com.example.Serializers.books

import kotlinx.serialization.Serializable

@Serializable
data class BookRequest(
    val title: String,
    val authors: List<Long>,
    val genres: List<Long>
)
