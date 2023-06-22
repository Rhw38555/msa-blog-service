package com.example.blogservice.controller

import com.example.blogservice.model.*
import com.example.blogservice.service.CommentService
import mu.KotlinLogging
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/comments")
class CommentController (
    private val commentService: CommentService,
){
    @PostMapping("")
    fun createComment(@RequestBody request: CreateCommentRequest, @AuthToken token:String) : CreateCommentResponse {
        val log = LoggerFactory.getLogger(javaClass)
        log.info("###### Start createComment Controller")
        return commentService.createComment(request, token)
    }

    @PostMapping("/{parentCommentId}/subcomments")
    fun createSubComment(
        @PathVariable parentCommentId: String,
        @RequestBody request: CreateCommentRequest,
        @AuthToken token:String,
    ) {
        commentService.createSubComment(parentCommentId, request, token)
    }

    @GetMapping("/posts/{postId}")
    fun getCommentsByPost(@PathVariable postId: String, @AuthToken token:String): CommentListResponse {
        return commentService.getCommentsByPost(postId, token)
    }

    @DeleteMapping("")
    fun deleteComment(@RequestBody request: DeleteCommentRequest, @AuthToken token:String): ResponseEntity<String> {
        commentService.deleteComment(request, token)
        return ResponseEntity.ok("정상 삭제되었습니다.")
    }

    @PutMapping("")
    fun updateComment(@RequestBody request: UpdateCommentRequest, @AuthToken token:String): CreateCommentResponse {
        return commentService.updateComment(request, token)
    }

}