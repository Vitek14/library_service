package com.example.service

import com.example.db.repository.BookRepository
import com.example.model.dto.book.BookResponse
import com.example.model.dto.book.CreateBookRequest
import com.example.model.dto.book.toResponse
import io.ktor.http.HttpStatusCode

class BookService(private val bookRepository: BookRepository) {

    suspend fun list(): List<BookResponse> =
        bookRepository.findAll().map { it.toResponse() }

    suspend fun getById(id: Long): BookResponse =
        bookRepository.findById(id)?.toResponse()
            ?: throw ApiException(HttpStatusCode.NotFound, "Book $id not found")

    suspend fun create(req: CreateBookRequest): BookResponse {
        if (req.title.isBlank()) throw ApiException(HttpStatusCode.BadRequest, "title is required")
        if (req.authors.isEmpty()) throw ApiException(HttpStatusCode.BadRequest, "at least one author is required")
        if (req.genres.isEmpty()) throw ApiException(HttpStatusCode.BadRequest, "at least one genre is required")
        return bookRepository.create(req.title, req.authors, req.genres).toResponse()
    }
}
