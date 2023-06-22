package com.example.userservice.controller

import com.example.userservice.config.JwtProperties
import com.example.userservice.domain.entity.User
import com.example.userservice.exception.ErrorResponse
import com.example.userservice.model.*
import com.example.userservice.service.UserService
import com.example.userservice.utils.JWTClaims
import com.example.userservice.utils.JwtUtils
import io.kotest.common.runBlocking
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.spec.style.Test
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import org.junit.jupiter.api.Assertions.*
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import javax.ws.rs.core.UriBuilder

class UserControllerTest: BehaviorSpec({

    val userService: UserService = mockk()
    val wetTestClient = WebTestClient.bindToController(UserController(userService)).build()

    given("정상 유저 정보가 들어온다면") {
        val request = UserSignUpRequest("dev@example.com", "dev", "dev1234")
        val response = UserSignUpResponse("john@example.com", "dev")
        val newUser = User(1, "john@example.com", "dev", "dev1234")
        coEvery { userService.signUp(request) } returns response

        `when`("컨트롤러를 호출한다"){
            wetTestClient.post()
                .uri("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.email").isEqualTo(response.email)
                .jsonPath("$.userName").isEqualTo(response.userName)
            then("정상 호출 된다"){
                coVerify(exactly = 1) { userService.signUp(any()) }
            }
        }
    }

    given("비정상 유저 정보가 들어온다면") {
        val request = mutableMapOf<String, String>()
        request["email22"] = "dev@gmail.com"
        request["userName22"] = "dev"
        request["password22"] = "dev1234"
        val response = UserSignUpResponse("john@example.com", "dev")
        val newUser = User(1, "john@example.com", "dev", "dev1234")

        `when`("컨트롤러를 호출한다"){
            wetTestClient.post()
                .uri("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest
            then("비정상 호출 된다"){
                coVerify(exactly = 0) { userService.signUp(any()) }
            }
        }
    }


    given("email로 유저 정보를 받을때") {
        val userEmail = "dev@gmail.com"
        val request = UserSignUpRequest("dev@example.com", "dev", "dev1234")
        val response = UserSignUpResponse("john@example.com", "dev")
        val newUser = User(1, "john@example.com", "dev", "dev1234")
        coEvery { userService.signUp(request) } returns response

        `when`("유저 정보 찾기를 호출한다"){
            wetTestClient.get()
                .uri("/api/v1/users")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.email").isEqualTo(response.email)
                .jsonPath("$.userName").isEqualTo(response.userName)
            then("정상 호출 된다"){
                coVerify(exactly = 1) { userService.findAllUsers() }
            }
        }
    }

    given("아무 정보도 받지 않고") {

        val userList = listOf(
            User(1, "john@example.com", "dev1", "password1"),
            User(2, "jane@example.com", "dev2", "password2")
        )

        val userFlow = flow { emit(userList) }
            .flatMapConcat {
                    userList -> flow { userList.forEach { emit(it) } } }

        val expectedResponse = GetUserListResponse(
            userList = userList.map { GetUserResponse(it) }
        )

        coEvery { userService.findAllUsers() } returns expectedResponse

        `when`("유저 전체 정보를 호출한다"){
            wetTestClient.get()
                .uri("/api/v1/users")
                .exchange()
                .expectStatus().isOk
                .expectBodyList(GetUserListResponse::class.java)
                .hasSize(1)
                .contains(expectedResponse)
            then("유저 전체 리스트가 정상 조회된다"){
                coVerify(exactly = 1) { userService.findAllUsers() }
            }
        }
    }


    given("수정 될 정보를 받고") {

        val request = UserSignUpRequest("dev@example.com", "dev", "dev1234")
        val updatedUser = User(1, "dev2@example.com", "dev2", "dev1234")
        val response = UserSignUpResponse(updatedUser)

        coEvery { userService.updateUser("1",request) } returns response

        `when`("컨트롤러를 호출한다"){
            wetTestClient.put()
                .uri("/api/v1/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.email").isEqualTo(response.email)
                .jsonPath("$.userName").isEqualTo(response.userName)
            then("정상 호출 된다"){
                coVerify(exactly = 1) { userService.updateUser(any(),any()) }
            }
        }
    }

    given("로그인 API를 호출할 때") {
        val loginRequest = LoginRequest("test@example.com", "password")
        val user = User(1, "test@example.com", "John", "password")
        val jwtClaims = JWTClaims(userId = 1, email = "test@example.com", userName = "John")
        val properties = JwtProperties(issuer = "my-issuer", subject = "auth", expiresTime = 3600, secret = "my-secret")
        val token = JwtUtils.createToken(jwtClaims, properties)
        val loginResponse = LoginResponse(email = "test@example.com", userName = "John", token = token)

        coEvery { runBlocking { userService.login(loginRequest, token) } } returns loginResponse

        `when`("로그인 API를 호출한다") {
            val response = wetTestClient.post()
                .uri("/api/v1/users//login")
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isOk
                .expectBody(LoginResponse::class.java)
                .returnResult()

            then("올바른 응답을 받는다") {
                val result = response.responseBody ?: fail("Response body is null")
                result shouldBe loginResponse
            }
        }
    }

})