package com.example.blogservice.utils

import com.example.blogservice.model.CacheUser
import com.example.userservice.exception.AuthNotFoundException
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestTemplate

object CommonUserUtils {

    fun getUserInfoByToken(redisTemplate: RedisTemplate<*,*>, restTemplate: RestTemplate, token: String): String{
        var cachedUser = redisTemplate.opsForValue().get(token)
        if(cachedUser == null){
            val headers = HttpHeaders()
            headers.set("Authorization", "Bearer "+token);
            val requestEntity = HttpEntity(null, headers)
            cachedUser = restTemplate.exchange(
                "http://localhost:8000/user-service/api/v1/users/token",
                HttpMethod.GET,
                requestEntity,
                CacheUser::class.java,
            ).body
        }
        cachedUser = cachedUser as CacheUser
        if(cachedUser == null || cachedUser.userName == null){
            throw AuthNotFoundException()
        }// if null

        return cachedUser.userName
    }

}