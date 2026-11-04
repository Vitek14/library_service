package com.example.service

import io.ktor.http.HttpStatusCode

open class ApiException(val status: HttpStatusCode, override val message: String) : RuntimeException(message)
