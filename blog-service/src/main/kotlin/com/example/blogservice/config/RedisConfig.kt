package com.example.blogservice.config

import com.example.blogservice.model.CacheUser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfig{

    @Bean
    fun redisConnectionFactory(): RedisConnectionFactory {
        return LettuceConnectionFactory()
    }

//    @Bean
//    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, CacheUser> {
//        val template = RedisTemplate<String, CacheUser>()
//        template.setConnectionFactory(connectionFactory)
//        template.keySerializer = StringRedisSerializer()
//        val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
//        ObjectMapper().readerFor(CacheUser::class.java).readValue<CacheUser>(jsonString)
//
////        template.valueSerializer = Jackson2JsonRedisSerializer(CacheUser::class.java)
//        template.valueSerializer = GenericJackson2JsonRedisSerializer(objectMapper)
//        return template
//    }

    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<*, *> {
        return RedisTemplate<Any, Any>().apply {
            this.setConnectionFactory(connectionFactory)
            this.keySerializer = StringRedisSerializer()
            // 직렬화 / 역직렬화시 json(dto) 형태의 값 형태로 저장하기 위한 설정
            this.valueSerializer = Jackson2JsonRedisSerializer(CacheUser::class.java).also {
                // 디폴트 생성자 처리를위해 명시적으로 코틀린 모듈 ObjectMapper 삽입
                it.setObjectMapper(jacksonObjectMapper())
            }
        }
    }

}