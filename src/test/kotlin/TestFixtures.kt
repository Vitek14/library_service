package com.example

import com.example.models.user.Roles
import com.example.models.user.User
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun testUser(
    id: Long = 1,
    login: String = "testuser",
    role: Roles = Roles.USER,
) = User(
    id = id, login = login, name = "Test", surname = "User",
    password = "hashed", role = role,
    createdAt = Clock.System.now(), updatedAt = Clock.System.now(),
)
