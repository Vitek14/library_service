package com.example.Serializers.books

import kotlinx.serialization.Serializable

@Serializable
data class AuthorBookRequest(val name: String)
