package com.example.wikinow.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class PostArticle(
    @DocumentId
    val id: String = "",

    @PropertyName("authorId")
    val authorId: String = "",

    @PropertyName("authorEmail")
    val authorEmail: String = "",

    @PropertyName("title")
    val title: String = "",

    @PropertyName("content")
    val content: String = "",

    @PropertyName("createdAt")
    @ServerTimestamp
    val createdAt: Date? = null
) {
    // No-argument constructor for Firestore deserialization
    constructor() : this("", "", "", "", "", null)
}