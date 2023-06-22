package com.example.blogservice.model

import com.example.blogservice.domain.entity.Image
import com.example.blogservice.domain.entity.Post
import com.example.blogservice.domain.entity.Video
import java.io.Serializable

data class SearchRequest(
    val keyword: String,
    val afterId: String,
)

data class GetImageResponse(
    val imageName: String,
    val url: String,
    val imageId: String,
) {
    companion object {
        operator fun invoke(images: List<Image>?): List<GetImageResponse> {
            return images?.map { image ->
                GetImageResponse(
                    imageName = image.imageName,
                    url = image.url,
                    imageId = image.imageId
                )
            } ?: emptyList()
        }
    }
}

data class GetVideoResponse(
    val videoName: String,
    val url: String,
    val videoId: String,
){
    companion object {
        operator fun invoke(videos: List<Video>?): List<GetVideoResponse> {
            return videos?.map { video ->
                GetVideoResponse(
                    videoName = video.videoName,
                    url = video.url,
                    videoId = video.videoId
                )
            } ?: emptyList()
        }
    }
}

data class GetPostResponse(
    val postId: String,
    val writerId: String,
    val isPrivate: Boolean,
    val title: String,
    val textContent: String?,
    val images: List<GetImageResponse>,
    val videos: List<GetVideoResponse>,
    val count: CountResponse,
//    val createdAt: String,
){
    companion object{
        operator fun invoke(posts: List<Post>) : List<GetPostResponse>{
            return posts.map { post ->
                GetPostResponse(
                    postId = post.id!!,
                    writerId = post.writerId,
                    isPrivate = post.isPrivate,
                    title = post.title,
                    textContent = post.content.text,
                    images = GetImageResponse(post.content.images),
                    videos = GetVideoResponse(post.content.videos),
                    count = with(post.count){
                        CountResponse(imageCount,videoCount,commentCount)
                    },
//                    createdAt = post.createdAt.toString()
                )
            } ?: emptyList()
        }
    }
}

data class DeletePostRequest(
    val postId: String,
) : Serializable


data class CreatePostRequest(
    val writerId: String,
    val isPrivate: Boolean,
    val title: String,
    val textContent: String,
)

data class CreatePostResponse(
    val postId: String,
    val writerId: String,
    val isPrivate: Boolean,
    val title: String,
    val textContent: String?,
    val images: List<ImagesResponse>,
    val videos: List<VideoResponse>,
    val counts: CountResponse,
)

data class ImagesResponse(
    val fileName: String,
    val contentType: String,
)

data class VideoResponse(
    val fileName: String,
    val contentType: String,
)

data class CountResponse(
    val imageCount: Int,
    val videoCount: Int,
    val commentCount: Int,
)