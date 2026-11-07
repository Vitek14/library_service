package com.example.utilities.auth
import de.mkammerer.argon2.Argon2Factory

object PasswordHasher {
    private val argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id)

    private const val ITERATIONS = 2
    private const val MEMORY_KIB = 19456 // 19 MiB
    private const val PARALLELISM = 1

    fun hashPassword(password: CharArray): String {
        try {
            return argon2.hash(ITERATIONS, MEMORY_KIB, PARALLELISM, password)
        } finally {
            argon2.wipeArray(password)
        }
    }

    fun verifyPassword(hash: String, passwordAttempt: CharArray): Boolean {
        try {
            return argon2.verify(hash, passwordAttempt)
        } finally {
            argon2.wipeArray(passwordAttempt)
        }
    }
}
