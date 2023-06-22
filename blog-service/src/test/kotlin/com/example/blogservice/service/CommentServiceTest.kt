package com.example.blogservice.service

import com.example.blogservice.domain.entity.Comment
import com.example.blogservice.domain.repository.CommentRepository
import com.example.blogservice.model.*
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import java.time.LocalDateTime
import java.util.LinkedList
import java.util.Optional

class CommentServiceTest : BehaviorSpec({
    val commentRepository = mockk<CommentRepository>()
    val mockRedisTemplate = mockk<RedisTemplate<String, CacheUser>>()
    val restTemplate = mockk<RestTemplate>()
    val headerToken = "token"
    val cacheUser1 = CacheUser(
        id = "1",
        email = "cacheUser@gmail.com",
        userName = "user1",
        token = headerToken,
    )
    val commentService = CommentService(commentRepository,mockRedisTemplate,restTemplate)

    beforeTest{
        every {mockRedisTemplate.opsForValue().get(headerToken) } returns null

        every {
            restTemplate.exchange(
                any<String>(),
                any<HttpMethod>(),
                any<HttpEntity<*>>(),
                any<Class<*>>()
            )
        } returns ResponseEntity.ok().body(cacheUser1)
    }

    Given("댓글 생성 요청이 주어졌을 때") {
        val createCommentRequest = CreateCommentRequest(
            postId = "1",
            writerId = "user1",
            text = "댓글 내용"
        )
        val newComment = Comment(
            id = "1",
            postId = "1",
            writerId = "user1",
            text = "댓글 내용"
        )

        every { commentRepository.save(any()) } returns newComment

        When("댓글을 생성하면") {
            val result = commentService.createComment(createCommentRequest, headerToken)

            Then("commentRepository의 save 메서드가 호출되고, 생성된 댓글 정보가 반환되어야 함") {
                verify(exactly = 1) { commentRepository.save(any()) }
                result shouldBe CreateCommentResponse(
                    commentId = "1",
                    postId = "1",
                    writerId = "user1",
                    text = "댓글 내용"
                )
            }
        }
    }

    Given("대댓글 생성 요청이 주어졌을 때") {
        val parentCommentId = "1"
        val createSubCommentRequest = CreateCommentRequest(
            postId = "1",
            writerId = "user1",
            text = "대댓글 내용"
        )
        val parentComment = Comment(
            id = parentCommentId,
            postId = "1",
            writerId = "user1",
            text = "댓글 내용"
        )
        val subComment = Comment(
            id = "2",
            postId = "1",
            writerId = "user1",
            text = "대댓글 내용",
            parentCommentId = parentCommentId
        )
        every { commentRepository.findById(parentCommentId) } returns Optional.of(parentComment)
        every { commentRepository.save(any()) } returns subComment

        When("대댓글을 생성하면") {
            commentService.createSubComment(parentCommentId, createSubCommentRequest, headerToken)

            Then("commentRepository의 findById와 save 메서드가 호출되어야 함") {
                verify(exactly = 1) { commentRepository.findById(parentCommentId) }
                verify(exactly = 1) { commentRepository.save(parentComment) }
            }

            And("대댓글이 부모 댓글의 subComments에 추가되어야 함") {
                parentComment.subComments shouldContain subComment
            }
        }
    }

    Given("댓글이 존재하는 경우") {
        val postId = "123"

        val rootComment1 = Comment(
            id = "1",
            postId = postId,
            writerId = "user1",
            text = "Root Comment 1",
            createdAt = LocalDateTime.now()
        )
        val rootComment2 = Comment(
            id = "2",
            postId = postId,
            writerId = "user2",
            text = "Root Comment 2",
            createdAt = LocalDateTime.now().plusMinutes(1)
        )
        val subComment1 = Comment(
            id = "3",
            postId = postId,
            writerId = "user3",
            text = "Sub Comment 1",
            parentCommentId = "1",
            createdAt = LocalDateTime.now().plusMinutes(30)
        )
        rootComment1.subComments = LinkedList<Comment>().apply { add(subComment1) }
        val subComment2 = Comment(
            id = "4",
            postId = postId,
            writerId = "user4",
            text = "Sub Comment 2",
            parentCommentId = "1",
            createdAt = LocalDateTime.now().plusMinutes(20)
        )
        rootComment1.subComments.add(subComment2)

        val subComment3 = Comment(
            id = "5",
            postId = postId,
            writerId = "user5",
            text = "Sub Comment 3",
            parentCommentId = "2",
            createdAt = LocalDateTime.now().plusMinutes(10)
        )
        rootComment2.subComments = LinkedList<Comment>().apply { add(subComment3) }

        val comments = listOf(rootComment1, rootComment2, subComment1, subComment2, subComment3)

        every { commentRepository.findByPostIdAndIsDeletedOrderByCreatedAtAsc(postId, false) } returns comments

        When("댓글 목록을 가져올 때") {
            val result = commentService.getCommentsByPost(postId, headerToken)

            Then("부모 댓글이 정렬된 순서로 포함되고, 대댓글도 순서대로 포함되어야 한다") {
                result.comments.size shouldBe 2

                val rootComment1Response = result.comments[0]
                val rootComment2Response = result.comments[1]

                rootComment1Response.commentId shouldBe rootComment1.id
                rootComment2Response.commentId shouldBe rootComment2.id

                rootComment1Response.subComments.size shouldBe 2
                rootComment2Response.subComments.size shouldBe 1

                val subComment1Response = rootComment1Response.subComments[0]
                val subComment2Response = rootComment1Response.subComments[1]
                val subComment3Response = rootComment2Response.subComments[0]

                subComment1Response.commentId shouldBe subComment1.id
                subComment2Response.commentId shouldBe subComment2.id
                subComment3Response.commentId shouldBe subComment3.id
            }
        }
    }

    Given("댓글이 존재하지 않는 경우") {
        val postId = "456"

        every { commentRepository.findByPostIdAndIsDeletedOrderByCreatedAtAsc(postId, false) } returns emptyList()

        When("댓글 목록을 가져올 때") {
            val result = commentService.getCommentsByPost(postId, headerToken)

            Then("빈 목록이 반환되어야 한다") {
                result.comments.size shouldBe 0
            }
        }
    }

    Given("유효한 댓글 삭제 요청이 있을 때") {
        val commentId = "123"
        val user = "user1"
        val request = DeleteCommentRequest(commentId, user)

        val comment = Comment(
            id = "1",
            postId = "1",
            writerId = "user1",
            text = "Root Comment 1",
            createdAt = LocalDateTime.now(),
            isDeleted = true,
        )

            When("댓글을 삭제한다") {
                every { commentRepository.findById(commentId) } returns Optional.of(comment)
                every { commentRepository.save(any()) } returns comment
                commentService.deleteComment(request, headerToken)

                Then("댓글이 삭제되었음을 표시한다") {
                    verify { commentRepository.findById(any()) }
                    verify { commentRepository.save(any()) }
                }
            }
    }

    Given("유효한 댓글 수정 요청이 있을 때") {
        val commentId = "123"
        val user = "user1"
        val text = "update"
        val request = UpdateCommentRequest(commentId, user, text)
        val comment = Comment(
            id = "1",
            postId = "1",
            writerId = "user1",
            text = text,
            createdAt = LocalDateTime.now(),
            isDeleted = true,
        )
        val commentResponse = CreateCommentResponse(comment)

        When("댓글을 삭제한다") {
            every { commentRepository.findById(commentId) } returns Optional.of(comment)
            every { commentRepository.save(any()) } returns comment
            val result = commentService.updateComment(request, headerToken)

            Then("댓글이 삭제되었음을 표시한다") {
                result shouldBe commentResponse
            }
        }
    }

})