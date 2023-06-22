package com.example.userservice.exception

sealed class ServerException (
    val code: Int,
    override val message: String,
) : RuntimeException(message)

data class FileException(
    override val message: String = "잘못된 파일입니다.",
) : ServerException(500, message)

data class PostNotFoundException(
    override val message: String = "찾을 수 없는 글입니다.",
) : ServerException(404, message)

data class AuthNotFoundException(
    override val message: String = "인증되지 않았습니다.",
) : ServerException(401, message)

data class CommentNotFoundException(
    override val message: String = "찾을 수 없는 댓글입니다.",
) : ServerException(404, message)
