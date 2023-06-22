package com.example.blogservice.domain.entity

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.mongodb.core.mapping.Field
import java.time.LocalDateTime
import javax.persistence.Column

abstract class BaseEntity (

    @CreatedDate
    @Field("created_at")
    var createdAt: LocalDateTime?=null,
)
