package com.example.model.entity

import java.time.Instant

data class Book(
    val id: Long,
    val title: String,
    val authors: List<Author>,
    val genres: List<Genre>,
    val createdAt: Instant,
    val updatedAt: Instant,
)
