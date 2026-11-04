package com.example.model.dto.book

import com.example.model.entity.Book

fun Book.toResponse() = BookResponse(
    id = id,
    title = title,
    authors = authors.map { it.name },
    genres = genres.map { it.name },
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)
