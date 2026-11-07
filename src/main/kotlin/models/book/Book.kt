package com.example.models.book

import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
data class Book @OptIn(ExperimentalTime::class) constructor(
    val id: Long,
    val title: String,
    val authors: List<Author>,
    val genres: List<Genre>,
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now()
)
