package com.example.userservice.service

import com.example.userservice.config.JwtProperties
import com.example.userservice.domain.entity.User
import com.example.userservice.domain.repository.UserRepository
import com.example.userservice.exception.InvalidJwtTokenException
import com.example.userservice.exception.NotFoundUserException
import com.example.userservice.exception.PasswordNotMatchedException
import com.example.userservice.exception.UserExistException
import com.example.userservice.model.*
import com.example.userservice.utils.BCryptUtils
import com.example.userservice.utils.JWTClaims
import com.example.userservice.utils.JwtUtils
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.collect
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import java.time.Duration

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
    private val loginReactiveRedisTemplate: ReactiveRedisTemplate<String, CacheUser>,
    private val jwtProperties: JwtProperties,
) {

    companion object{
        private val CACHE_TTL = Duration.ofMinutes(1)
    }

    @Transactional
    suspend fun signUp(request: UserSignUpRequest) : UserSignUpResponse {
        return with(request){
            userRepository.findByEmail(request.email)?.let{
                throw UserExistException()
            }
            val user = User(
                email = email,
                password = BCryptUtils.hash(password),
                userName = userName,
            )
            val newUser = userRepository.save(user)!!

            UserSignUpResponse(
                email = newUser.email,
                userName = newUser.userName,
            )
        }
    }

    suspend fun findUserByEmail(userEmail: String): GetUserResponse? {
        return userRepository.findByEmail(userEmail)?.let{
             with(it){
                GetUserResponse(
                    email = email,
                    userName = userName,
                )
            }
        }
    }

    suspend fun findAllUsers() : GetUserListResponse{
        return GetUserListResponse(userRepository.findAll())
    }

    @Transactional
    suspend fun updateUser(id:String, request: UserSignUpRequest): UserSignUpResponse? {
        return userRepository.findByEmail(request.email)?.let{
            with(it){
                if(id.toLong() != it.id){
                    throw NotFoundUserException()
                }
                val newUser: User = it.copy(email=request.email, userName = request.userName,
                    password = BCryptUtils.hash(request.password))
                UserSignUpResponse(userRepository.save(newUser)!!)
            }
        }
    }

    suspend fun login(loginRequest: LoginRequest, token:String?) : LoginResponse{

        // hint 방식, cache 정보 존재하면 캐시정보 이용
//        val cachedUser = loginReactiveRedisTemplate.opsForValue().get(loginRequest.email).awaitFirstOrNull()
        val cachedUser = if (token != null) loginReactiveRedisTemplate.opsForValue().get(token)
            .awaitFirstOrNull() else null
        return if (cachedUser != null) {
            LoginResponse(
                email = cachedUser.email,
                userName = cachedUser.userName,
                token = JwtUtils.createToken(
                    JWTClaims(
                        userId = cachedUser.id!!.toLong(),
                        email = cachedUser.email,
                        userName = cachedUser.userName
                    ),
                    jwtProperties
                )
            )
        } else {
            val user = userRepository.findByEmail(loginRequest.email)
                ?: throw NotFoundUserException()
            val verified = BCryptUtils.verify(loginRequest.password, user.password)
            if (!verified) {
                throw PasswordNotMatchedException()
            }
            // login
            val jwtClaim = JWTClaims(
                userId = user.id!!,
                email = user.email,
                userName = user.userName,
            )
            val token = JwtUtils.createToken(jwtClaim, jwtProperties)
            // user 객체 그대로 캐시 저장
//            loginReactiveRedisTemplate.opsForValue().set(user.email, CacheUser(user, token)).awaitFirstOrNull()
            loginReactiveRedisTemplate.opsForValue().set(token, CacheUser(user, token)).awaitFirstOrNull()
            LoginResponse(
                email = user.email,
                userName = user.userName,
                token = token,
            )
        }
    }

    suspend fun logout(token: String){
//        val decodedJWT = JwtUtils.decode(token, jwtProperties.secret, jwtProperties.issuer)
//            ?: throw InvalidJwtTokenException()
//        val email = decodedJWT.getClaim("email").asString()
//        val cachedUser = loginReactiveRedisTemplate.opsForValue().get(email).awaitFirstOrNull()
        val cachedUser = loginReactiveRedisTemplate.opsForValue().get(token).awaitFirstOrNull()
        if (cachedUser != null) {
//            loginReactiveRedisTemplate.opsForValue().delete(email).awaitFirstOrNull()
            loginReactiveRedisTemplate.opsForValue().delete(token).awaitFirstOrNull()
        }
    }

    suspend fun getUserInfoByToken(token: String): CacheUser {
        val decodedJWT = JwtUtils.decode(token, jwtProperties.secret, jwtProperties.issuer) ?: throw InvalidJwtTokenException()
        val email = decodedJWT.getClaim("email").asString()
        val userName = decodedJWT.getClaim("userName").asString()
        return CacheUser(id="id", email = email, userName = userName, token = token)
    }

}