package com.example

import com.example.utilities.auth.PasswordHasher

import kotlin.test.*

class PasswordHasherTest {

    @Test
    fun `hashPassword produces argon2id hash`() {
        val hash = PasswordHasher.hashPassword("secret123".toCharArray())
        assertTrue(hash.startsWith("\$argon2id"))
        assertNotEquals("secret123", hash)
    }

    @Test
    fun `verifyPassword returns true for the correct password`() {
        val hash = PasswordHasher.hashPassword("secret123".toCharArray())
        assertTrue(PasswordHasher.verifyPassword(hash, "secret123".toCharArray()))
    }

    @Test
    fun `verifyPassword returns false for a wrong password`() {
        val hash = PasswordHasher.hashPassword("secret123".toCharArray())
        assertFalse(PasswordHasher.verifyPassword(hash, "wrong".toCharArray()))
    }

    @Test
    fun `hash uses a random salt`() {
        val h1 = PasswordHasher.hashPassword("pass".toCharArray())
        val h2 = PasswordHasher.hashPassword("pass".toCharArray())
        assertNotEquals(h1, h2)
        assertTrue(PasswordHasher.verifyPassword(h1, "pass".toCharArray()))
        assertTrue(PasswordHasher.verifyPassword(h2, "pass".toCharArray()))
    }

    @Test
    fun `hashPassword wipes the input array`() {
        val password = "secret123".toCharArray()
        PasswordHasher.hashPassword(password)
        assertTrue(password.all { it == '\u0000' })
    }
}
