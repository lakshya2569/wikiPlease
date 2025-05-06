package com.example.wikinow.data.model
import com.example.wikinow.data.model.Thumbnail
data class SearchResponse(
    val pages: List<WikiPage>
)

data class WikiPage(
    val id: Long,
    val key: String,
    val title: String,
    val excerpt: String,
    val description: String?,
    val thumbnail: Thumbnail?
)

