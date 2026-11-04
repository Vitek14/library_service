package com.example.service

import com.example.db.repository.BookRepository
import com.example.db.repository.RentRequestRepository
import com.example.db.repository.UserRepository
import com.example.model.dto.rent.RentRequestResponse
import com.example.model.entity.RentRequest
import io.ktor.http.HttpStatusCode
import java.sql.SQLException
import java.time.Instant
import java.time.temporal.ChronoUnit


class RentService(
    private val rentRequestRepository: RentRequestRepository,
    private val bookRepository: BookRepository,
    private val userRepository: UserRepository,
) {

    suspend fun createRequest(userId: Long, bookId: Long): RentRequestResponse {
        bookRepository.findById(bookId)
            ?: throw ApiException(HttpStatusCode.NotFound, "Book $bookId not found")

        if (rentRequestRepository.findApprovedByBook(bookId) != null) {
            throw ApiException(HttpStatusCode.Conflict, "Book $bookId is already rented by someone else")
        }
        if (rentRequestRepository.findActiveByBookAndUser(bookId, userId) != null) {
            throw ApiException(HttpStatusCode.Conflict, "You already have an active request for this book")
        }
        if (rentRequestRepository.existsDeclinedByBookAndUser(bookId, userId)) {
            throw ApiException(HttpStatusCode.Forbidden, "Your previous request for this book was declined")
        }

        val request = try {
            rentRequestRepository.create(bookId, userId)
        } catch (e: SQLException) {
            if (e.sqlState == "23505") {
                throw ApiException(HttpStatusCode.Conflict, "Active request for this book already exists")
            }
            throw e
        }
        return request.toResponse()
    }

    suspend fun listPending(): List<RentRequestResponse> =
        rentRequestRepository.findAllPending().map { it.toResponse() }

    suspend fun approve(requestId: Long, expiresAt: Instant): RentRequestResponse {
        val minExpiresAt = Instant.now().plus(1, ChronoUnit.HOURS)
        if (expiresAt.isBefore(minExpiresAt)) {
            throw ApiException(HttpStatusCode.BadRequest, "expiresAt must be at least 1 hour in the future")
        }

        rentRequestRepository.findById(requestId)
            ?: throw ApiException(HttpStatusCode.NotFound, "Rent request $requestId not found")

        val ok = rentRequestRepository.approve(requestId, expiresAt)
        if (!ok) throw ApiException(HttpStatusCode.Conflict, "Rent request $requestId is not pending")

        return rentRequestRepository.findById(requestId)!!.toResponse()
    }

    suspend fun decline(requestId: Long): RentRequestResponse {
        rentRequestRepository.findById(requestId)
            ?: throw ApiException(HttpStatusCode.NotFound, "Rent request $requestId not found")

        val ok = rentRequestRepository.decline(requestId)
        if (!ok) throw ApiException(HttpStatusCode.Conflict, "Rent request $requestId is not pending")

        return rentRequestRepository.findById(requestId)!!.toResponse()
    }

    private suspend fun RentRequest.toResponse(): RentRequestResponse {
        val book = bookRepository.findById(bookId)
        val user = userRepository.findById(userId)
        return RentRequestResponse(
            id = id,
            bookId = bookId,
            bookTitle = book?.title ?: "<missing>",
            userId = userId,
            userLogin = user?.login ?: "<missing>",
            status = status.name,
            requestedAt = requestedAt.toString(),
            approvedAt = approvedAt?.toString(),
            declinedAt = declinedAt?.toString(),
            expiresAt = expiresAt?.toString(),
        )
    }
}
