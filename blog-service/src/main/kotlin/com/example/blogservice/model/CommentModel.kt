package com.example.blogservice.model

import com.example.blogservice.domain.entity.Comment


data class CommentListResponse(
    val comments: List<CommentResponse>
)

data class CommentResponse(
    val commentId: String,
    val postId: String,
    val writerId: String,
    val text: String,
    val parentCommentId: String?,
    var subComments: MutableList<CommentResponse>,
    val createdAt: String,
)

data class CreateSubCommentRequest(
    val postId: String,
    val writerId: String,
    val text: String,
    val parentId: String,
)

data class DeleteCommentRequest(
    val commentId: String,
    val writerId: String,
)

data class UpdateCommentRequest(
    val commentId: String,
    val writerId: String,
    val text: String,
)

data class CreateCommentRequest(
    val postId: String,
    val writerId: String,
    val text: String,
)

data class CreateCommentResponse(
    val commentId: String,
    val postId: String,
    val writerId: String,
    val text: String,
){
    companion object{
        operator fun invoke(comment: Comment) : CreateCommentResponse{
            return with(comment){
                CreateCommentResponse(
                    commentId = id!!,
                    postId = postId,
                    writerId = writerId,
                    text = text,
                )
            }
        }
    }
}