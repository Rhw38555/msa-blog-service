package com.example.userservice.controller

import com.example.userservice.model.*
import com.example.userservice.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/users")
class UserController (
    private val userService: UserService,
){
    @PostMapping("/signup")
    suspend fun signUp(@RequestBody request: UserSignUpRequest) : UserSignUpResponse{
        return userService.signUp(request)
    }

    @GetMapping("/email/{userEmail}")
    suspend fun getUserByEmail(@PathVariable userEmail: String) : GetUserResponse?{
        return userService.findUserByEmail(userEmail)
    }

    @GetMapping("")
    suspend fun getAllUsers() : GetUserListResponse?{
        return userService.findAllUsers()
    }

    @PutMapping("{id}")
    suspend fun updateUser(@PathVariable id: String, @RequestBody request: UserSignUpRequest) : UserSignUpResponse?{
        return userService.updateUser(id,request)
    }

    @PostMapping("/login")
    suspend fun login(@RequestBody request: LoginRequest, @AuthToken token: String?) : LoginResponse{
        return userService.login(request, token)
    }

    @DeleteMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    suspend fun logout(@AuthToken token: String){
        userService.logout(token)
    }


    @GetMapping("/token")
    suspend fun getUserInfoByToken(@AuthToken token: String) : CacheUser{
        return userService.getUserInfoByToken(token)
    }

}