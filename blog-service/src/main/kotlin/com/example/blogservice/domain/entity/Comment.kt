package com.example.blogservice.domain.entity

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.mongodb.core.mapping.Field
import java.time.LocalDateTime
import java.util.LinkedList
import javax.persistence.Id

data class Comment(
    @Id
    val id: String? = null,
    @Field
    val postId: String,
    @Field
    val writerId: String,
    @Field
    var text: String,
    @Field
    var parentCommentId: String? = null,  // 대댓글인 경우 상위 댓글의 ID
    @Field
    var subComments: LinkedList<Comment> = LinkedList(),  // 대댓글 목록
    @Field
    var isDeleted: Boolean = false,
    @CreatedDate
    @Field("created_at")
    var createdAt: LocalDateTime?=null,
    )
//) : BaseEntity()