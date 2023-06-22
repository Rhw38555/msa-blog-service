package com.example.blogservice.model.enums

enum class FileType {
    IMAGE, VIDEO;

    companion object{
        operator fun invoke(type:String) = valueOf(type.uppercase())
    }
}