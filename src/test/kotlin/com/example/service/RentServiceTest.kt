package com.example.service
import com.example.db.repository.BookRepository
import com.example.db.repository.RentRequestRepository
import com.example.db.repository.UserRepository
import com.example.model.entity.Author
import com.example.model.entity.Book
import com.example.model.entity.RentRequest
import com.example.model.entity.User
import com.example.model.enums.RentRequestStatus
import com.example.model.enums.Role
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RentServiceTest {

    private lateinit var rentRequestRepository: RentRequestRepository
    private lateinit var bookRepository: BookRepository
    private lateinit var userRepository: UserRepository
    private lateinit var rentService: RentService

    private val sampleBook = Book(
        id = 1L, title = "The Hobbit",
        authors = listOf(Author(1L, "Tolkien")),
        genres = emptyList(),
        createdAt = Instant.now(), updatedAt = Instant.now(),
    )
    private val sampleUser = User(
        id = 10L, name = "Ivan", surname = "Ivanov", login = "ivan",
        passwordHash = "hashed", role = Role.USER,
        createdAt = Instant.now(), updatedAt = Instant.now(),
    )

    @BeforeTest
    fun setup() {
        rentRequestRepository = mockk()
        bookRepository = mockk()
        userRepository = mockk()
        rentService = RentService(rentRequestRepository, bookRepository, userRepository)

        coEvery { bookRepository.findById(any()) } returns sampleBook
        coEvery { userRepository.findById(any()) } returns sampleUser
    }

    // createRequest
    @Test
    fun `createRequest succeeds`() = runBlocking {
        coEvery { bookRepository.findById(1L) } returns sampleBook
        coEvery { rentRequestRepository.findApprovedByBook(1L) } returns null
        coEvery { rentRequestRepository.findActiveByBookAndUser(1L, 10L) } returns null
        coEvery { rentRequestRepository.existsDeclinedByBookAndUser(1L, 10L) } returns false
        coEvery { rentRequestRepository.create(1L, 10L) } returns rent(1L, status = RentRequestStatus.PENDING)

        val result = rentService.createRequest(userId = 10L, bookId = 1L)

        assertEquals("PENDING", result.status)
        assertEquals("The Hobbit", result.bookTitle)
        assertEquals("ivan", result.userLogin)
    }

    @Test
    fun `createRequest fails when book not found`() = runBlocking {
        coEvery { bookRepository.findById(99L) } returns null

        val ex = assertFailsWith<ApiException> { rentService.createRequest(10L, 99L) }
        assertEquals(HttpStatusCode.NotFound, ex.status)
    }

    @Test
    fun `createRequest fails when book already approved to someone`() = runBlocking {
        coEvery { bookRepository.findById(1L) } returns sampleBook
        coEvery { rentRequestRepository.findApprovedByBook(1L) } returns rent(99L, status = RentRequestStatus.APPROVED)

        val ex = assertFailsWith<ApiException> { rentService.createRequest(10L, 1L) }
        assertEquals(HttpStatusCode.Conflict, ex.status)
    }

    @Test
    fun `createRequest fails when user already has active request`() = runBlocking {
        coEvery { bookRepository.findById(1L) } returns sampleBook
        coEvery { rentRequestRepository.findApprovedByBook(1L) } returns null
        coEvery { rentRequestRepository.findActiveByBookAndUser(1L, 10L) } returns rent(1L, status = RentRequestStatus.PENDING)

        val ex = assertFailsWith<ApiException> { rentService.createRequest(10L, 1L) }
        assertEquals(HttpStatusCode.Conflict, ex.status)
    }

    @Test
    fun `createRequest fails when previous request was declined`() = runBlocking {
        coEvery { bookRepository.findById(1L) } returns sampleBook
        coEvery { rentRequestRepository.findApprovedByBook(1L) } returns null
        coEvery { rentRequestRepository.findActiveByBookAndUser(1L, 10L) } returns null
        coEvery { rentRequestRepository.existsDeclinedByBookAndUser(1L, 10L) } returns true

        val ex = assertFailsWith<ApiException> { rentService.createRequest(10L, 1L) }
        assertEquals(HttpStatusCode.Forbidden, ex.status)
    }

    // approve
    @Test
    fun `approve fails when expiresAt is less than 1 hour ahead`() = runBlocking {
        val tooSoon = Instant.now().plus(30, ChronoUnit.MINUTES)

        val ex = assertFailsWith<ApiException> { rentService.approve(1L, tooSoon) }
        assertEquals(HttpStatusCode.BadRequest, ex.status)
    }

    @Test
    fun `approve fails when request not found`() = runBlocking {
        coEvery { rentRequestRepository.findById(42L) } returns null

        val ex = assertFailsWith<ApiException> {
            rentService.approve(42L, Instant.now().plus(7, ChronoUnit.DAYS))
        }
        assertEquals(HttpStatusCode.NotFound, ex.status)
    }

    @Test
    fun `approve fails when request is not pending`() = runBlocking {
        val future = Instant.now().plus(7, ChronoUnit.DAYS)
        coEvery { rentRequestRepository.findById(1L) } returns rent(1L, status = RentRequestStatus.APPROVED)
        coEvery { rentRequestRepository.approve(1L, future) } returns false

        val ex = assertFailsWith<ApiException> { rentService.approve(1L, future) }
        assertEquals(HttpStatusCode.Conflict, ex.status)
    }

    @Test
    fun `approve succeeds and returns updated request`() = runBlocking {
        val future = Instant.now().plus(7, ChronoUnit.DAYS)
        coEvery { rentRequestRepository.findById(1L) } returnsMany listOf(
            rent(1L, status = RentRequestStatus.PENDING),
            rent(1L, status = RentRequestStatus.APPROVED, expiresAt = future),
        )
        coEvery { rentRequestRepository.approve(1L, future) } returns true

        val result = rentService.approve(1L, future)

        assertEquals("APPROVED", result.status)
        coVerify { rentRequestRepository.approve(1L, future) }
    }

    // decline
    @Test
    fun `decline fails when request not found`() = runBlocking {
        coEvery { rentRequestRepository.findById(42L) } returns null

        val ex = assertFailsWith<ApiException> { rentService.decline(42L) }
        assertEquals(HttpStatusCode.NotFound, ex.status)
    }

    @Test
    fun `decline fails when request is not pending`() = runBlocking {
        coEvery { rentRequestRepository.findById(1L) } returns rent(1L, status = RentRequestStatus.APPROVED)
        coEvery { rentRequestRepository.decline(1L) } returns false

        val ex = assertFailsWith<ApiException> { rentService.decline(1L) }
        assertEquals(HttpStatusCode.Conflict, ex.status)
    }

    @Test
    fun `decline succeeds`() = runBlocking {
        coEvery { rentRequestRepository.findById(1L) } returnsMany listOf(
            rent(1L, status = RentRequestStatus.PENDING),
            rent(1L, status = RentRequestStatus.DECLINED),
        )
        coEvery { rentRequestRepository.decline(1L) } returns true

        val result = rentService.decline(1L)

        assertEquals("DECLINED", result.status)
    }

    private fun rent(
        id: Long,
        status: RentRequestStatus,
        expiresAt: Instant? = null,
    ) = RentRequest(
        id = id, bookId = 1L, userId = 10L, status = status,
        requestedAt = Instant.now(),
        approvedAt = if (status == RentRequestStatus.APPROVED) Instant.now() else null,
        declinedAt = if (status == RentRequestStatus.DECLINED) Instant.now() else null,
        expiresAt = expiresAt,
    )
}
