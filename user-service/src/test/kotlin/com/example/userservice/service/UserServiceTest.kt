package com.example.userservice.service

import com.example.userservice.config.JwtProperties
import com.example.userservice.domain.entity.User
import com.example.userservice.domain.repository.UserRepository
import com.example.userservice.exception.UserExistException
import com.example.userservice.model.*
import com.example.userservice.utils.BCryptUtils
import com.example.userservice.utils.JWTClaims
import com.example.userservice.utils.JwtUtils
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.common.runBlocking
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.flow.*
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.data.redis.core.ReactiveRedisTemplate
import reactor.core.publisher.Mono

@DataR2dbcTest
class UserServiceTest:BehaviorSpec({
    val userRepository = mockk<UserRepository>()
    val jwtProperties = mockk<JwtProperties>()
    val loginReactiveRedisTemplate = mockk<ReactiveRedisTemplate<String, CacheUser>>()
    val userService = UserService(userRepository,loginReactiveRedisTemplate, jwtProperties)

    given("유저 정보가 들어온다면") {
        val request = UserSignUpRequest(
            email = "dev@gmail.com", userName = "dev", password = "dev1234",
        )
        val registeredUser = User(
            id = 1, email = "dev@gmail.com" ,userName = "dev", password = BCryptUtils.hash("dev1234")
        )

        `when`("존재하지않는 이메일로 회원가입이 성공한다면") {
            // mockk
            coEvery { userRepository.findByEmail(request.email) } returns null
            // mockk
            coEvery { userRepository.save(any()) }  returns registeredUser

            then("에러없이 회원가입이 성공해야한다") {
                shouldNotThrowAny {
                    runBlocking { userService.signUp(request) }
                }
            }// then
        }// when

        `when`("존재하는 이메일로 회원가입이 실패한다면") {
            val existingUser: User = User(
                id = registeredUser.id, email = registeredUser.email,
                userName = registeredUser.userName, password = registeredUser.password
            )

            coEvery { userRepository.findByEmail(request.email) } returns existingUser

            then("UserExistException이 발생해야됨") {
                shouldThrow<UserExistException> {
                    runBlocking { userService.signUp(request) }
                }
            }// then
        }// when

    }// given


    given("사용자 email이 있다면"){
        val userEmail = "dev@gamil.com"
        val user = User(
            1, userEmail, "dev", "dev1234",
        )
        val userResponse = GetUserResponse(user)

        `when`("사용자 정보를 찾는다"){

            then("사용자가 존재한다"){
                // mockk
                coEvery { userRepository.findByEmail(userEmail) } returns user
                val findUser = runBlocking { userService.findUserByEmail(userEmail) }
                findUser shouldBe GetUserResponse(user)
            }

            then("사용자가 존재하지 않는다"){
                // mockk
                coEvery { userRepository.findByEmail(userEmail) } returns null
                val findUser = runBlocking { userService.findUserByEmail(userEmail) }
                findUser shouldBe null
            }

        }
    }// given


    given("아무 정보를 안주어도"){

        val user1 = User(1, "dev1@gmail.com", "dev1", "devq1")
        val user2 = User(2, "dev2@gmail.com", "dev2", "devq2")
        val userList = listOf(
            user1,
            user2,
        )

        val userFlow = flow { emit(userList) }
            .flatMapConcat {
                    userList -> flow { userList.forEach { emit(it) } } }

        `when`("모든 사용자 정보를 가져온다"){
            coEvery { userRepository.findAll() } returns userFlow
            val findUserList = runBlocking { userService.findAllUsers() }

            then("모든 사용자 정보를 확인할 수 있다"){
                val expectedResponse = GetUserListResponse(
                    userList = userList.map { GetUserResponse(it) }
                )
                // GetUserListResponse 객체가 같
                findUserList shouldBe expectedResponse
            }
        }
    }

    given("수정할 유저 정보가 들어오면") {
        val request = UserSignUpRequest(
            email = "dev2@gmail.com", userName = "dev2", password = "dev1234",
        )
        val user = User(
            id = 1, email = "dev1@gmail.com" ,userName = "dev", password = BCryptUtils.hash("dev1234")
        )

        // mock
        coEvery {  userRepository.findByEmail(any()) } returns user

        `when`("사용자 정보를 수정한다"){

            val updatedUser = User(
                id = 1, email = "dev2@gmail.com" ,userName = "dev2", password = BCryptUtils.hash("dev1234")
            )

            val updatedUserSignResponse = UserSignUpResponse(updatedUser)
            // mock
            coEvery {  userRepository.save(any()) } returns updatedUser

            then("사용자 정보가 수정된다"){
                shouldNotThrowAny {
                    val result = runBlocking { userService.updateUser("1", request) }
                    result shouldBe updatedUserSignResponse
                }
            }
        }
    }

    given("로그인 정보가 주어질때") {

        val properties = JwtProperties(
            issuer = "rhw",
            subject = "auth",
            expiresTime = 3600,
            secret = "my-secret"
        )

        val jwtClaims = JWTClaims(
            userId = 1,
            email = "test@example.com",
            userName="Test User",
        )

        every { jwtProperties.issuer } returns properties.issuer
        every { jwtProperties.subject } returns properties.subject
        every { jwtProperties.expiresTime } returns properties.expiresTime
        every { jwtProperties.secret } returns properties.secret

        `when`("캐시가 존재하면") {
            val email = "test@example.com"
            val cachedUser = CacheUser(
                User(id = 1, email = email, userName = "Test User", password = "password"),
                JwtUtils.createToken(jwtClaims, properties),
            )

            // Mock cached user
            coEvery { loginReactiveRedisTemplate.opsForValue().get(cachedUser.token) } returns Mono.just(cachedUser)

            then("캐시로 로그인 토큰을 대체한다") {
                val loginRequest = LoginRequest(email, "password")
                val expectedResponse = LoginResponse(email, "Test User", JwtUtils.createToken(jwtClaims, properties))

                runBlocking {
                    val response = userService.login(loginRequest, cachedUser.token)
                    response shouldBe expectedResponse
                }
            }
        }

        `when`("캐시가 존재하지 않을때") {
            val email = "test@example.com"
            val user = User(id = 1, email = email, userName = "Test User", password = BCryptUtils.hash("password"))
            val loginRequest = LoginRequest(email, "password")
            val token = "token"

            val cachedUser = CacheUser(
                user,
                JwtUtils.createToken(jwtClaims, properties),
            )

            // Mock
            coEvery { userRepository.findByEmail(email) } returns user

            // Mock no cached
            coEvery { loginReactiveRedisTemplate.opsForValue().get(token) } returns Mono.empty()

            // Mock
            coEvery { loginReactiveRedisTemplate.opsForValue().set(any(), any()) } returns Mono.just(true)

            then("유저 데이터를 반환하고 토큰을 캐시에 저장한다") {
                val expectedResponse = LoginResponse(email, "Test User", JwtUtils.createToken(jwtClaims, properties))

                runBlocking {
                    val response = userService.login(loginRequest, "token")
                    response shouldBe expectedResponse
                }
            }
        }
    }

    given("캐시가 존재할 때"){

        val properties = JwtProperties(
            issuer = "rhw",
            subject = "auth",
            expiresTime = 3600,
            secret = "my-secret"
        )

        val jwtClaims = JWTClaims(
            userId = 1,
            email = "test@example.com",
            userName="Test User",
        )

        every { jwtProperties.issuer } returns properties.issuer
        every { jwtProperties.secret } returns properties.secret

        val token = JwtUtils.createToken(jwtClaims, properties)
        val email = "test@example.com"
        val cachedUser = CacheUser(
            User(id = 1, email = email, userName = "Test User", password = "password"),
            token
        )

        `when`("로그아웃을 한다."){

            // Mock
            coEvery { loginReactiveRedisTemplate.opsForValue().get(cachedUser.token) } returns Mono.just(cachedUser)
            coEvery { loginReactiveRedisTemplate.opsForValue().delete(cachedUser.token) } returns Mono.just(true)

            then("로그아웃이 정상 수행되면 캐시가 삭제된다."){
                shouldNotThrowAny {
                    runBlocking {
                        userService.logout(token)
                    }
                }
            }
        }
    }

})