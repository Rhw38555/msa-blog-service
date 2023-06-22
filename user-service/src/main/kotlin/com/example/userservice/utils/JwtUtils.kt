package com.example.userservice.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import com.example.userservice.config.JwtProperties
import java.util.*

object JwtUtils{

    fun createToken(jwtClaims: JWTClaims, properties: JwtProperties) =
        JWT.create()
            .withIssuer(properties.issuer)
            .withSubject(properties.subject)
            .withIssuedAt(Date())
            .withExpiresAt(Date(Date().time + properties.expiresTime * 1000))
            .withClaim("userId", jwtClaims.userId)
            .withClaim("email", jwtClaims.email)
            .withClaim("userName", jwtClaims.userName)
            .sign(Algorithm.HMAC256(properties.secret))

    fun decode(token: String?, secret: String, issuer: String): DecodedJWT {
        val algorithm = Algorithm.HMAC256(secret)

        val verifier = JWT.require(algorithm)
            .withIssuer(issuer)
            .build()

        return verifier.verify(token)
    }

}


data class JWTClaims(
    val userId: Long,
    val email: String,
    val userName: String,
)
