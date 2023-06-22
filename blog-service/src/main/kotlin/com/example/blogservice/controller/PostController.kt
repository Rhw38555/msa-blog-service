package com.example.blogservice.controller

import com.example.blogservice.model.*
import com.example.blogservice.service.PostService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile


@RestController
@RequestMapping("/api/v1/posts")
class PostController (
    val postService: PostService,
){

    @PostMapping("/search")
    fun searchPostsByKeyword(@RequestBody searchRequest: SearchRequest, @AuthToken token: String): List<GetPostResponse>{
        return postService.searchPostsByKeyword(searchRequest, token)
    }

    @GetMapping("")
    fun getPosts(@AuthToken token: String) : List<GetPostResponse>{
        return postService.findAllByWriterId(token)
    }

    @PostMapping("")
    fun createPost(@RequestPart request: CreatePostRequest,
                   @RequestPart("images") images : List<MultipartFile>,
                   @RequestPart("videos") videos : List<MultipartFile>,
                 @AuthToken token:String): CreatePostResponse {
        return postService.savePost(request, images, videos, token)
    }

    @DeleteMapping("")
    fun deletePost(@RequestBody request: DeletePostRequest, @AuthToken token:String) : ResponseEntity<String> {
        postService.delete(request, token)
        return ResponseEntity.ok("정상 삭제되었습니다.")
    }

}