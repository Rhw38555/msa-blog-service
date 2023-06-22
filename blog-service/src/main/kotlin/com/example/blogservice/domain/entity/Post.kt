package com.example.blogservice.domain.entity

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.time.LocalDateTime
import javax.persistence.Id

@Document(collection = "posts")
data class Post (
    @Id
    var id: String? = null,
    val writerId: String,
    val isPrivate: Boolean,
    val title: String,
    val content: Content,
    val count: Count,
)
//    @CreatedDate
//    @Field("created_at")
//    var createdAt: LocalDateTime?=null,
//)
//) :BaseEntity()

data class Content(
    val text: String?,
    val images: List<Image>?,
    val videos: List<Video>?
)

data class Count(
    val imageCount: Int,
    val videoCount: Int,
    val commentCount: Int
)
