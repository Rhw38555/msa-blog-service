package com.example.blogservice.service

import com.example.blogservice.domain.entity.*
import com.example.blogservice.model.*
import com.example.blogservice.domain.repository.PostRepository
import com.example.blogservice.kafka.producer.PostProducer
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.client.RestTemplate
import java.util.Optional

class PostServiceTest : BehaviorSpec({

    val mockPostRepository = mockk<PostRepository>()
    val mockRedisTemplate = mockk<RedisTemplate<String, CacheUser>>()
    val restTemplate = mockk<RestTemplate>()
    val kafkaPostProducer = mockk<PostProducer>()
    val elasticService = mockk<ElasticService>()
    val postService = PostService(mockPostRepository, mockRedisTemplate, restTemplate, kafkaPostProducer, elasticService)

    given("토큰 정보가 들어왔을때"){
        val headerToken = "token"
        val cacheUser = CacheUser(
            id = "1",
            email = "cacheUser@gmail.com",
            userName = "writer1",
            token = headerToken,
        )

        val posts = listOf(Post(
            id="test1",
                writerId = "writer1",
        isPrivate = false,
        title = "테스트 게시물",
        content = Content(
            text = "이것은 테스트 게시물입니다.",
            images = listOf(Image("image.jpg", "/path/to/image.jpg", "writer1_image.jpg")),
            videos = listOf(Video("video.mp4", "/path/to/video.mp4", "writer1_video.mp4"))
        ), count = Count(imageCount = 1, videoCount = 1, commentCount = 0)),
            Post(
                id="test2",
                writerId = "writer1",
                isPrivate = false,
                title = "테스트 게시물2",
                content = Content(
                    text = "이것은 테스트 게시물2입니다.",
                    images = listOf(Image("image2.jpg", "/path/to/image2.jpg", "writer1_image2.jpg")),
                    videos = listOf(Video("video2.mp4", "/path/to/video2.mp4", "writer1_video2.mp4"))
                ), count = Count(imageCount = 1, videoCount = 1, commentCount = 0)))

        val postResponse = GetPostResponse(posts)

        `when`("내 블로그 게시글을 전체 조회한다"){

            every {mockRedisTemplate.opsForValue().get(headerToken) } returns null

            every {
                restTemplate.exchange(
                    any<String>(),
                    any<HttpMethod>(),
                    any<HttpEntity<*>>(),
                    any<Class<*>>()
                )
            } returns ResponseEntity.ok().body(cacheUser)

            every { mockPostRepository.findAllByWriterId(cacheUser.userName) } returns posts
            val result = postService.findAllByWriterId(headerToken)

            then("내 블로그 게시글이 조회된다."){
                result shouldNotBe null
                result shouldBe postResponse
            }
        }
    }

    given("파일과 함께 게시물 저장 요청이 있을 때") {

        val headerToken = "token"
        val cacheUser = CacheUser(
            id = "1",
            email = "cacheUser@gmail.com",
            userName = "writer1",
            token = headerToken,
        )

        val request = CreatePostRequest(
            writerId = "writer1",
            isPrivate = false,
            title = "테스트 게시물",
            textContent = "이것은 테스트 게시물입니다."
        )
        val imageFiles = listOf(
            MockMultipartFile("image", "image.jpg", "image/jpeg", byteArrayOf(1, 2, 3)),
        )

        val videoFiles = listOf(
            MockMultipartFile("video", "video.mp4", "video/mp4", byteArrayOf(4, 5, 6))
        )

        val post = Post(
            id = "1",
            writerId = "writer1",
            isPrivate = false,
            title = "테스트 게시물",
            content = Content(
                text = "이것은 테스트 게시물입니다.",
                images = listOf(Image("image.jpg", "/path/to/image.jpg", "writer1_image.jpg")),
                videos = listOf(Video("video.mp4", "/path/to/video.mp4", "writer1_video.mp4"))
            ),
            count = Count(imageCount = 1, videoCount = 1, commentCount = 0)
        )

        val postResponse = CreatePostResponse(
            postId = "1",
            writerId = "writer1",
            isPrivate = false,
            title = "테스트 게시물",
            textContent = "이것은 테스트 게시물입니다.",
            images = listOf(ImagesResponse("image.jpg","image/jpeg")),
            videos = listOf(VideoResponse("video.mp4","video/mp4")),
            counts = CountResponse(1,1,0)
        )

        `when`("savePost 함수를 호출할 때") {

            every {mockRedisTemplate.opsForValue().get(headerToken) } returns null

            every {
                restTemplate.exchange(
                    any<String>(),
                    any<HttpMethod>(),
                    any<HttpEntity<*>>(),
                    any<Class<*>>()
                )
            } returns ResponseEntity.ok().body(cacheUser)

            every { mockPostRepository.save(any()) } returns post
            every { kafkaPostProducer.sendPost("post-topic", post) } just Runs
            val response = postService.savePost(request, imageFiles, videoFiles, headerToken)

            then("게시물이 저장되고 응답이 반환되어야 합니다") {
                response.writerId shouldBe postResponse.writerId
                response.isPrivate shouldBe postResponse.isPrivate
                response.title shouldBe postResponse.title
                response.textContent shouldBe postResponse.textContent
                response.images.size shouldBe postResponse.images.size
                response.images[0].fileName shouldBe postResponse.images[0].fileName
                response.images[0].contentType shouldBe postResponse.images[0].contentType
                response.videos.size shouldBe 1
                response.videos[0].fileName shouldBe postResponse.videos[0].fileName
                response.videos[0].contentType shouldBe postResponse.videos[0].contentType
                response.counts.imageCount shouldBe postResponse.counts.imageCount
                response.counts.videoCount shouldBe postResponse.counts.videoCount
                response.counts.commentCount shouldBe postResponse.counts.commentCount
            }
        }
    }

    given("게시물 삭제 요청이 있을 때") {
        val request = DeletePostRequest(
            postId = "posdId",
        )

        val headerToken = "token"
        val cacheUser = CacheUser(
            id = "1",
            email = "cacheUser@gmail.com",
            userName = "test1",
            token = headerToken,
        )

        `when`("캐시에 토큰이 존재하고 게시물을 삭제할 때"){
            // header token redis cache 확인
            every {mockRedisTemplate.opsForValue().get(headerToken) } returns cacheUser
            every {mockPostRepository.findById(request.postId)} returns Optional.of(Post(id=request.postId,
            writerId = "test1", isPrivate = false, title="test1",
                content = Content(text="test1", images = null, videos = null),
                count = Count(imageCount = 0, videoCount = 0, commentCount = 0)))
            every {mockPostRepository.deleteById(request.postId)} just Runs

            postService.delete(request, headerToken)
            then("게시물이 삭제되어야 합니다"){
                verify { mockPostRepository.findById(request.postId) }
                verify { mockPostRepository.deleteById(request.postId) }
            }
        }

        `when`("캐시에 토큰이 존재하지않고 게시물을 삭제할 때"){
            // header token redis cache 확인
            every {mockRedisTemplate.opsForValue().get(headerToken) } returns null

            every {
                restTemplate.exchange(
                    any<String>(),
                    any<HttpMethod>(),
                    any<HttpEntity<*>>(),
                    any<Class<*>>()
                )
            } returns ResponseEntity.ok().body(cacheUser)
            val post = Post(id=request.postId,
                writerId = "test1", isPrivate = false, title="test1",
                content = Content(text="test1", images = null, videos = null),
                count = Count(imageCount = 0, videoCount = 0, commentCount = 0))

            every {mockPostRepository.findById(request.postId)} returns Optional.of(post)
            every {mockPostRepository.deleteById(request.postId)} just Runs

            val result = postService.delete(request, headerToken)

            postService.delete(request, headerToken)

            then("게시물이 삭제되어야 합니다"){
                verify { mockPostRepository.findById(request.postId) }
                verify { mockPostRepository.deleteById(request.postId) }
            }
        }


    }

})