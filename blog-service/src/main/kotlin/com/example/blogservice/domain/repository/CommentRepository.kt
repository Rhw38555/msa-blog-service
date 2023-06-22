package com.example.blogservice.domain.repository

import com.example.blogservice.domain.entity.Comment
import org.springframework.data.mongodb.repository.MongoRepository

interface CommentRepository : MongoRepository<Comment, String> {
    fun findByPostIdAndIsDeletedOrderByCreatedAtAsc(postId: String, isDeleted: Boolean) : List<Comment>
    fun findByParentCommentId(commentId: String) : List<Comment>
}