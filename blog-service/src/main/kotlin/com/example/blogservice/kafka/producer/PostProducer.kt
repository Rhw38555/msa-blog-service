package com.example.blogservice.kafka.producer

import com.example.blogservice.domain.entity.Post
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class PostProducer (private val kafkaTemplate: KafkaTemplate<String, Post>) {

    fun sendPost(topic: String, post: Post) {
//        kafkaTemplate.send(topic, post.writerId, post)
        kafkaTemplate.send(topic, "post", post)
    }
}