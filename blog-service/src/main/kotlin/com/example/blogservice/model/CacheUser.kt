package com.example.blogservice.model

import java.io.Serializable

data class CacheUser(
    val id: String,
    val email: String,
    val userName: String,
    val token: String,
): Serializable