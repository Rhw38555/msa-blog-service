package com.example.userservice.utils

import com.auth0.jwt.interfaces.DecodedJWT
import com.example.userservice.config.JwtProperties
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Assertions.*

class JwtUtilsTest : BehaviorSpec({
    Given("토큰 정보가 주어지면"){
        val jwtClaims = JWTClaims(
            userId = 1,
            email = "dev@gmail.com",
            userName="test123",
        )
        val properties = JwtProperties(
            issuer = "rhw",
            subject = "auth",
            expiresTime = 3600,
            secret = "my-secret"
        )

        When("토큰을 생성한다"){
            val token = JwtUtils.createToken(jwtClaims, properties)
            Then("정상적으로 토큰이 생성된다."){
                token shouldNotBe null
            }// Then
        }// When
    }// Given

    Given("토큰이 주어지면"){

        val jwtClaims = JWTClaims(
            userId = 1,
            email = "dev@gmail.com",
            userName="test123",
        )
        val properties = JwtProperties(
            issuer = "rhw",
            subject = "auth",
            expiresTime = 3600,
            secret = "my-secret"
        )
        val token = JwtUtils.createToken(jwtClaims, properties)
        When("토큰을 복호화 한다"){
            val decode: DecodedJWT = JwtUtils.decode(token, secret=properties.secret, issuer=properties.issuer)
            Then("토큰 복호화 정보를 확인한다"){
                with(decode){
                    claims["userId"]!!.toString() shouldBe jwtClaims.userId.toString()
                    claims["email"]!!.toString().replace("\"","") shouldBe jwtClaims.email
                    claims["userName"]!!.toString().replace("\"","") shouldBe jwtClaims.userName
                }
            }
        }
    }
})