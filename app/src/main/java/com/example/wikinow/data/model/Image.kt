package com.example.wikinow.data.model

data class Image(
    val artist: Artist,
    val credit: Credit,
    val description: Description,
    val file_page: String,
    val image: ImageX,
    val license: License,
    val structured: Structured,
    val thumbnail: Thumbnail,
    val title: String,
    val wb_entity_id: String
)