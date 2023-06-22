package com.example.blogservice.service

import com.example.blogservice.config.LocalDateTimeSerializer
import com.example.blogservice.domain.entity.Post
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.elasticsearch.action.DocWriteResponse
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class ElasticService(private val restHighLevelClient: RestHighLevelClient) {

    private val INDEX_NAME = "post"
    private val filedList = listOf("writerId", "title", "content.text",
        "content.image.imageName", "content.video.videoName")
    fun savePostToElasticsearch(post: Post) {
        // 객체 변환
        val objectMapper = ObjectMapper()
        val postJsonString = objectMapper.registerModule(JavaTimeModule()).writeValueAsString(post)
//        val postJsonString = objectMapper.writeValueAsString(post)

        val indexRequest = IndexRequest(INDEX_NAME)
            .source(postJsonString, XContentType.JSON)

        // Elasticsearch에 데이터 저장
        val indexResponse: IndexResponse = restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT)
        if (indexResponse.result == DocWriteResponse.Result.CREATED ||
            indexResponse.result == DocWriteResponse.Result.UPDATED
        ) {
            // save success
        } else {
            // save fail
        }
    }

    fun searchPostsByKeyword(keyword: String, afterId:String): List<Post> {
        val searchRequest = SearchRequest(INDEX_NAME)
        val searchSourceBuilder = SearchSourceBuilder()
        val boolQueryBuilder = QueryBuilders.boolQuery()

        // 필드 별 검색
//        for(field in filedList){
//            boolQueryBuilder.should(QueryBuilders.matchQuery(field, keyword))
//        }

//        boolQueryBuilder.should(QueryBuilders.matchQuery("title", keyword))
//        searchSourceBuilder.query(boolQueryBuilder)
//            .size(10)

        // id 이후부터 가져오도록 설정
//        if (afterId != null && afterId != "") {
//            searchSourceBuilder.searchAfter(arrayOf(afterId))
//        }
        searchSourceBuilder.query(QueryBuilders.matchQuery("title", keyword))
        // 검색
        searchRequest.source(searchSourceBuilder)
        val searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT)

        val gsonBuilder = GsonBuilder()
        gsonBuilder.registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeSerializer())
        val gson: Gson = gsonBuilder.setPrettyPrinting().create()
        val hits = searchResponse.hits
        val posts = hits.map { hit ->
            var postJsonString = hit.sourceAsString
            // JSON 문자열을 Gson을 사용하여 JsonObject로 변환
//            val jsonObject: JsonObject = gson.fromJson(postJsonString, JsonObject::class.java)
//            // "createdAt" 값 추출 및 변환
//            val createdAtArray: JsonArray = jsonObject.getAsJsonArray("createdAt")
//            val stringBuilder = StringBuilder()
//            for (element in createdAtArray) {
//                val value = element.asString
//                stringBuilder.append(String.format("%02d", value.toInt()))
//            }
//            val createdAt: String = stringBuilder.toString()
//            // 변환된 값으로 JsonObject 업데이트
//            jsonObject.addProperty("createdAt", createdAt)
//            // 변경된 JsonObject를 다시 JSON 문자열로 변환
//            val updatedJsonString = gson.toJson(jsonObject)
//            gson.fromJson(updatedJsonString, Post::class.java)
            gson.fromJson(postJsonString, Post::class.java)
        }
        return posts
    }
}
