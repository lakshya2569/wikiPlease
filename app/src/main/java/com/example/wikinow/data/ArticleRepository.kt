package com.example.wikinow.data
import com.example.wikinow.data.model.PostArticle
import com.example.wikinow.data.model.Tfa
import com.example.wikinow.data.model.WikiPage
import kotlinx.coroutines.flow.Flow


interface ArticleRepository
{

    suspend fun getFeaturedArticle(year: String, month: String, day: String, language: String): Flow<Result<List<Tfa>>>
    suspend fun searchPages(query: String, language: String = "en"): Flow<Result<List<WikiPage>>>

    suspend fun createPostArticle(article: PostArticle): Flow<Result<String>>
    suspend fun getUserArticles(userId: String): Flow<Result<List<PostArticle>>>
    suspend fun getAllArticles(): Flow<Result<List<PostArticle>>>
}