package com.example.blogservice.service

import com.example.blogservice.domain.entity.Comment
import com.example.blogservice.domain.repository.CommentRepository
import com.example.blogservice.model.*
import com.example.blogservice.utils.CommonUserUtils
import com.example.userservice.exception.AuthNotFoundException
import com.example.userservice.exception.CommentNotFoundException
import com.example.userservice.exception.PostNotFoundException
import mu.KotlinLogging
import org.slf4j.LoggerFactory
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.time.LocalDateTime
import java.util.*

@Service
class CommentService(
    private val commentRepository: CommentRepository,
    private val redisTemplate: RedisTemplate<*, *>,
    private val restTemplate: RestTemplate,
) {
    fun createComment(request: CreateCommentRequest, token: String) : CreateCommentResponse {
        val log = LoggerFactory.getLogger(javaClass)
        log.info("###### Start createComment Service")
        val cachedUser = CommonUserUtils.getUserInfoByToken(redisTemplate, restTemplate, token)
        if(request.writerId != cachedUser) {
            throw AuthNotFoundException()
        }
        val comment = with(request) {
            Comment(postId = postId, writerId = writerId, text = text, createdAt = LocalDateTime.now())
        }
        val newComment = commentRepository.save(comment)
        return CreateCommentResponse(newComment)
    }

    fun createSubComment(parentCommentId: String, request: CreateCommentRequest, token: String) {

        val cachedUser = CommonUserUtils.getUserInfoByToken(redisTemplate, restTemplate, token)
        if(request.writerId != cachedUser) {
            throw AuthNotFoundException()
        }
        val parentComment = commentRepository.findById(parentCommentId)
        parentComment.ifPresent {
            val subComment = Comment(
                postId = request.postId,
                writerId = request.writerId,
                text = request.text,
                parentCommentId = parentCommentId,
                createdAt = LocalDateTime.now(),
            )
            val savedSubComment = commentRepository.save(subComment)
            it.subComments.addLast(savedSubComment)
            commentRepository.save(it)
        }
    }

    fun getCommentsByPost(postId: String, token: String): CommentListResponse {

        val cachedUser = CommonUserUtils.getUserInfoByToken(redisTemplate, restTemplate, token)
        val comments = commentRepository.findByPostIdAndIsDeletedOrderByCreatedAtAsc(postId, false)
        val rootComments = mutableListOf<CommentResponse>()

        // 모든 댓글을 Map에 저장하여 ID를 기준으로 접근할 수 있도록 합니다.
        val commentMap = comments.associateBy { it.id }

        for (comment in comments) {
            if (comment.parentCommentId == null) {
                val commentResponse = createCommentResponse(comment, commentMap)
                rootComments.add(commentResponse)
            }
        }

        return CommentListResponse(rootComments)
    }

    fun deleteComment(request: DeleteCommentRequest, token: String) {
        val cachedUser = CommonUserUtils.getUserInfoByToken(redisTemplate, restTemplate, token)
        if(request.writerId != cachedUser) {
            throw AuthNotFoundException()
        }
        val comment = commentRepository.findById(request.commentId).orElseThrow { CommentNotFoundException() }
        comment.isDeleted = true
        commentRepository.save(comment)
    }

    fun updateComment(request: UpdateCommentRequest, token: String) : CreateCommentResponse{
        val cachedUser = CommonUserUtils.getUserInfoByToken(redisTemplate, restTemplate, token)
        if(request.writerId != cachedUser) {
            throw AuthNotFoundException()
        }
        val comment = commentRepository.findById(request.commentId).orElseThrow { CommentNotFoundException() }
        comment.text = request.text
        return CreateCommentResponse(commentRepository.save(comment))
    }

    // 대댓글을 정렬을 위한 재귀 함수
    private fun createCommentResponse(comment: Comment, commentMap: Map<String?, Comment>): CommentResponse {

        val subComments = comment.subComments.map {
            createCommentResponse(it, commentMap)
        }.toMutableList()
        return CommentResponse(
            commentId = comment.id!!,
            postId = comment.postId,
            writerId = comment.writerId,
            text = comment.text,
            parentCommentId = comment.parentCommentId,
            subComments = subComments,
            createdAt = comment.createdAt.toString()
        )
    }



}