package com.example.userservice.config

import com.example.userservice.model.CacheUser
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer

/*
비동기식 Redis를 사용하는데 있어서 기존의 @Cacheable, @CacheEvict 등의 어노테이션은 사용되지 않는다.
따라서 이 어노테이션의 역할을 별도의 기능으로 추가해야됨 -> Cache Hit 전략을 채택
 */
@Configuration
class ReactiveRedisConfig {

    /**
     * ReactiveRedisTemplate를 생성하기 위한 공통 로직을 선언한 메소드
     * @param factory Lettuce를 이용한 Non-blocking connection factory
     * @param clazz ReactiveRedisTemplate에서 사용할 클래스
     */
    private fun <T> commonReactiveRedisTemplate(
        factory: ReactiveRedisConnectionFactory?,
        clazz: Class<T>
    ): ReactiveRedisTemplate<String, T> {
        val keySerializer = StringRedisSerializer()
        val redisSerializer = Jackson2JsonRedisSerializer(clazz)
            .apply {
                setObjectMapper(
                    jacksonObjectMapper()
                        .registerModule(JavaTimeModule())
                )
            }

        val serializationContext = RedisSerializationContext
            .newSerializationContext<String, T>()
            .key(keySerializer)
            .hashKey(keySerializer)
            .value(redisSerializer)
            .hashValue(redisSerializer)
            .build()

        return ReactiveRedisTemplate(factory!!, serializationContext)
    }

    // user에 대한 reactive redis template
    @Bean
    fun loginReactiveRedisTemplate(
        factory: ReactiveRedisConnectionFactory,
    ): ReactiveRedisTemplate<String, CacheUser> = commonReactiveRedisTemplate(factory, CacheUser::class.java)
}