package com.example.userservice.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("jwt")
data class JwtProperties(
    val issuer: String,
    val subject: String,
    val expiresTime: Long,
    val secret: String,
)
