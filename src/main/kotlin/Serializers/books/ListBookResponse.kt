package com.example.Serializers.books

//import com.example.Serializers.dto.books.BookDto
import com.example.models.book.Book
import kotlinx.serialization.Serializable

@Serializable
data class ListBooksResponse(val books: List<Book>)
