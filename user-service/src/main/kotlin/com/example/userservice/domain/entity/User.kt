package com.example.userservice.domain.entity

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("users")
data class User (
    @Id
    val id: Long?=null,

    @Column
    val email: String,

    @Column
    val userName: String,

    @Column
    val password: String,

) : BaseEntity()
