package com.example.Serializers.dto.books

import kotlinx.serialization.Serializable

@Serializable
data class AuthorDto(val id: Long, val name: String)
