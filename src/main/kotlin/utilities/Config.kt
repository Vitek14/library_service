package com.example.utilities

fun envOrProperty(key: String): String? =
    System.getProperty(key) ?: System.getenv(key)
