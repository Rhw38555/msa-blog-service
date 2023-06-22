package com.example.userservice.model

import com.example.userservice.domain.entity.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList


data class GetUserResponse(
    val email:String,
    val userName:String,
){
    companion object{
        operator fun invoke(user: User) = with(user){
            GetUserResponse(
                email = email,
                userName = userName,
            )
        }
    }
}

data class GetUserListResponse(
    val userList: List<GetUserResponse>
){
    companion object{
        // 일시중단 함수 toList때문에 suspend 붙임
        suspend operator fun invoke(userFlow: Flow<User?>) = with(userFlow){

            val list: MutableList<GetUserResponse> = mutableListOf()
            userFlow.collect { user ->
                user?.let {
                    val getUserResponse = GetUserResponse(it)
                    list.add(getUserResponse)
                }
            }
            GetUserListResponse(list)
        }
    }
}