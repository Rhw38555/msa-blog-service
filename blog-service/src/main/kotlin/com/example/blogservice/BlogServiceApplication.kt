package com.example.blogservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@ConfigurationPropertiesScan
@SpringBootApplication
class BlogServiceApplication

fun main(args: Array<String>) {
	runApplication<BlogServiceApplication>(*args)

}
