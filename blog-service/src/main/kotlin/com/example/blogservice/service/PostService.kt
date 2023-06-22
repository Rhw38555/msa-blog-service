package com.example.blogservice.service

import com.example.blogservice.domain.entity.*
import com.example.blogservice.model.*
import com.example.blogservice.model.enums.FileType
import com.example.blogservice.domain.repository.PostRepository
import com.example.blogservice.kafka.producer.PostProducer
import com.example.blogservice.utils.CommonUserUtils.getUserInfoByToken
import com.example.userservice.exception.AuthNotFoundException
import com.example.userservice.exception.PostNotFoundException
import org.springframework.data.redis.core.RedisTemplate

import org.springframework.stereotype.Service
import org.springframework.util.CollectionUtils
import org.springframework.util.StringUtils
import org.springframework.web.client.RestTemplate
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class PostService (
    private val postRepository: PostRepository,
    private val redisTemplate: RedisTemplate<*, *>,
    private val restTemplate: RestTemplate,
    private val kafkaPostProducer: PostProducer,
    private val elasticService: ElasticService,
){
    fun searchPostsByKeyword(searchRequest: SearchRequest, token:String) : List<GetPostResponse> {
        val cachedUser = getUserInfoByToken(redisTemplate, restTemplate, token)
        val posts = elasticService.searchPostsByKeyword(searchRequest.keyword, searchRequest.afterId)
        return GetPostResponse(posts)
    }

    fun findAllByWriterId(token: String): List<GetPostResponse> {
        // TODO 페이징 형식 성능 개선
        val cachedUser = getUserInfoByToken(redisTemplate, restTemplate, token)
        val posts:List<Post> = postRepository.findAllByWriterId(cachedUser)
        return GetPostResponse(posts)
    }

    fun savePost(postRequest: CreatePostRequest, files: List<MultipartFile>,
                 videos:List<MultipartFile>, token:String): CreatePostResponse {

        val cachedUser = getUserInfoByToken(redisTemplate, restTemplate, token)
        if(postRequest.writerId != cachedUser){
            throw AuthNotFoundException()
        }// if null

        // 파일 저장 및 객체 생성
        val imageList = saveFile(postRequest.writerId ,files, FileType.IMAGE.name)
        val videoList = saveFile(postRequest.writerId ,videos, FileType.VIDEO.name)
        var imageResponseList:MutableList<ImagesResponse> = mutableListOf<ImagesResponse>()
        var videoResponseList:MutableList<VideoResponse> = mutableListOf<VideoResponse>()


        var imageListContent:List<Image> = mutableListOf<Image>().apply {
            for(image in imageList){
                add(Image(image["fileName"]!! ,image["path"]!! ,image["id"]!!))
                imageResponseList.add(ImagesResponse(image["originName"]!!,image["contentType"]!!))
            }
        }
        var videoListContent:List<Video> = mutableListOf<Video>().apply {
            for(video in videoList){
                add(Video(video["fileName"]!! ,video["path"]!! ,video["id"]!!))
                videoResponseList.add(VideoResponse(video["originName"]!!,video["contentType"]!!))
            }
        }

        val content = Content(text = postRequest.textContent, images = imageListContent, videos = videoListContent,)

        val count = Count(imageCount = imageListContent.size, videoCount = videoListContent.size, commentCount = 0,)

        val post = Post(
            writerId = postRequest.writerId,
            isPrivate = postRequest.isPrivate,
            title = postRequest.title,
            content = content,
            count = count,
//            createdAt = LocalDateTime.now(),
        )

        val newPost = postRepository.save(post)
        writePostToKafka("post-topic", newPost)
        return with(newPost){
            CreatePostResponse(
                postId = newPost.id!!,
                writerId = writerId,
                isPrivate = isPrivate,
                title = title,
                textContent = content.text,
                images = imageResponseList.map {
                    ImagesResponse(it.fileName, it.contentType)
                },
                videos = videoResponseList.map{
                    VideoResponse(it.fileName, it.contentType)
                },
                counts = CountResponse(
                    count.imageCount,
                    count.videoCount,
                    count.commentCount,
                ),
            )
        }

    }

    fun delete(request: DeletePostRequest, token: String) {
        val cachedUser = getUserInfoByToken(redisTemplate, restTemplate, token)
        // token userName 정보와 writer 정보가 동일할 때 삭제
        val post = postRepository.findById(request.postId).orElseThrow { PostNotFoundException() }
        if(post.writerId == cachedUser){
            postRepository.deleteById(request.postId)
        }
    }


    private fun saveFile(writer:String, files: List<MultipartFile>, type:String) : List<Map<String,String>> {

        val listMap = mutableListOf<Map<String, String>>()

        // 파일이 존재할 경우
        if(!CollectionUtils.isEmpty(files)){
            val now: LocalDateTime = LocalDateTime.now()
            val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
            val currentDate = now.format(dateTimeFormatter)

            // 프로젝트 디렉토리 경로
            val absolutePath: String = File("").getAbsolutePath() + File.separator + File.separator
            val path = "images" + File.separator + currentDate
            val file: File = File(path)
            if(!file.exists()){
                file.mkdirs()
            }

            for(file in files){
                // 확장자 추출
                var originalFileExtention:String? = null

                originalFileExtention = if(type == FileType.IMAGE.name){
                    validateImageFileExtension(file)
                }else{
                    validateVideoFileExtension(file)
                }

                // 파일 저장
                val newFileName: String = System.nanoTime().toString() + "_" + file.originalFilename
                        file.originalFilename + originalFileExtention
                val savedFile = File(absolutePath + path + File.separator + newFileName)
                file.transferTo(savedFile)
                savedFile.setWritable(true)
                savedFile.setReadable(true)

                val map = mutableMapOf<String,String>()
                map["fileName"] = newFileName
                map["path"] = savedFile.absolutePath
                map["id"] = writer + newFileName
                map["originName"] = file.originalFilename.toString()
                map["contentType"] = file.contentType.toString()
                listMap.add(map)
            }
        }
        return listMap
    }



    private fun validateImageFileExtension(videoFile: MultipartFile) : String? {
        val allowedExtensions = listOf("jpg", "jpeg", "png") // 허용할 확장자 목록

        val originalFilename = StringUtils.cleanPath(videoFile.originalFilename.orEmpty())
        val fileExtension = StringUtils.getFilenameExtension(originalFilename)

        if (fileExtension !in allowedExtensions) {
            throw IllegalArgumentException("올바른 이미지 파일 형식이 아닙니다.")
        }

        return fileExtension
    }

    private fun validateVideoFileExtension(videoFile: MultipartFile) : String? {
        val allowedExtensions = listOf("mp4", "avi", "mov") // 허용할 확장자 목록

        val originalFilename = StringUtils.cleanPath(videoFile.originalFilename.orEmpty())
        val fileExtension = StringUtils.getFilenameExtension(originalFilename)

        if (fileExtension !in allowedExtensions) {
            throw IllegalArgumentException("올바른 동영상 파일 형식이 아닙니다.")
        }

        return fileExtension
    }

    private fun writePostToKafka(topic: String, post: Post) {
        kafkaPostProducer.sendPost(topic, post)
    }


}