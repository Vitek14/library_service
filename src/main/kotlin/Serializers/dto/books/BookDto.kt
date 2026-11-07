package com.example.Serializers.dto.books

import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
data class BookDto @OptIn(ExperimentalTime::class) constructor(
    val id: Long,
    val title: String,
    val authors: List<AuthorDto>,
    val genres: List<GenreDto>,
    val createdAt: Instant,
    val updatedAt: Instant
)
