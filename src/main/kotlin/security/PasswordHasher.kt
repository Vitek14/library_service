package com.example.security

import de.mkammerer.argon2.Argon2Factory

class PasswordHasher {
    private val argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id)

    fun hash(password: String): String {
        val chars = password.toCharArray()
        return try {
            argon2.hash(2, 19456, 1, chars)
        } finally {
            argon2.wipeArray(chars)
        }
    }

    fun verify(password: String, hash: String): Boolean {
        val chars = password.toCharArray()
        return try {
            argon2.verify(hash, chars)
        } finally {
            argon2.wipeArray(chars)
        }
    }
}