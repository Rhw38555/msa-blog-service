package com.example.blogconfigservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.config.server.EnableConfigServer


@SpringBootApplication
@EnableConfigServer
class BlogConfigServiceApplication

fun main(args: Array<String>) {
    runApplication<BlogConfigServiceApplication>(*args)
}
