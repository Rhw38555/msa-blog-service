package com.example.userservice.model

import com.example.userservice.domain.entity.User

data class CacheUser(
    val id: String,
    val email: String,
    val userName: String,
    val token: String,
){
    companion object{
        operator fun invoke(user: User, token:String) = with(user){
            CacheUser(id.toString(),email,userName,token)
        }
    }
}

data class LoginRequest(
    val email: String,
    val password: String,
)

data class LoginResponse(
    val email: String,
    val userName: String,
    val token: String,
)

data class UserSignUpRequest(
    val email: String,
    val userName: String,
    val password: String,
)


data class UserSignUpResponse(
    val email: String,
    val userName: String,
){
    companion object {
        operator fun invoke(user: User) = with(user){
            UserSignUpResponse(
                email=email,
                userName=userName,
            )
        }
    }
}