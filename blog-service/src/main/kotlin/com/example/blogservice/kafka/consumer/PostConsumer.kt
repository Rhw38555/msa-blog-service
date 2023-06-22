package com.example.blogservice.kafka.consumer

import com.example.blogservice.domain.entity.Post
import com.example.blogservice.service.ElasticService
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component


@Component
class PostConsumer(
    private val elasticService: ElasticService,
) {

    @KafkaListener(topics = ["post-topic"], groupId = "post")
    fun consumePost(post: Post) {
        elasticService.savePostToElasticsearch(post)
    }
}