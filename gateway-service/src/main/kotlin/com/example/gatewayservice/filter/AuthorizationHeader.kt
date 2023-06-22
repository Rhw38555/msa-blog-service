package com.example.gatewayservice.filter


import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.apache.http.HttpHeaders
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class AuthorizationHeaderFilter(var env: Environment) :
    AbstractGatewayFilterFactory<AuthorizationHeaderFilter.Config>(Config::class.java) {
    class Config

    // login -> token -> users (with token) -> header(include token)
    override fun apply(config: Config?): GatewayFilter {
        // api 호출 시 토큰 확인 및 전달
        return GatewayFilter { exchange, chain ->
            val request: ServerHttpRequest = exchange.getRequest()
            if (!request.headers.containsKey(HttpHeaders.AUTHORIZATION)) return@GatewayFilter onError(
                exchange,
                "no authorization header",
                HttpStatus.UNAUTHORIZED
            )
            val authorizationHeader =
                request.headers[HttpHeaders.AUTHORIZATION]!![0]
            val jwt = authorizationHeader.replace("Bearer", "")
            if (!isJwtValid(jwt)) return@GatewayFilter onError(
                exchange,
                "JWT token is not valid",
                HttpStatus.UNAUTHORIZED
            )
            chain.filter(exchange)
        }
    }

    private fun isJwtValid(jwt: String): Boolean {
        var returnValue = true
        var userName: String? = null
        try {
            val algorithm = Algorithm.HMAC256(env.getProperty("jwt.secret"))
            val verifier = JWT.require(algorithm)
                .withIssuer(env.getProperty("jwt.issuer"))
                .build()

            userName = verifier.verify(jwt).getClaim("userName").asString()

        } catch (ex: Exception) {
            returnValue = false
        }
        if (userName == null || userName.isEmpty()) {
            returnValue = false
        }
        return returnValue
    }

    // Mono, Flux -> Spring WebFlux
    private fun onError(exchange: ServerWebExchange, err: String, httpStatus: HttpStatus): Mono<Void?> {
        val response = exchange.response
        response.statusCode = httpStatus
        return response.setComplete()
    }
}