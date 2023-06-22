package com.example.blogservice.domain.repository

import com.example.blogservice.domain.entity.Post
import org.springframework.data.mongodb.repository.MongoRepository

interface PostRepository : MongoRepository<Post, String> {
    fun findAllByWriterId(writerId:String) : List<Post>
}