package com.example.wikinow.data
import com.google.firebase.firestore.FirebaseFirestore
import com.example.wikinow.data.model.PostArticle
import com.example.wikinow.data.model.Tfa
import com.example.wikinow.data.model.WikiPage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import com.google.firebase.firestore.ktx.firestore
import okio.IOException
import retrofit2.HttpException
import kotlinx.coroutines.tasks.await
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.Query.Direction
import kotlinx.coroutines.tasks.await

import com.google.firebase.firestore.ktx.firestore

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

import com.google.firebase.firestore.ktx.firestore


class ArticleRepositoryImpl(
    private val api: Api,
    private val firestore: FirebaseFirestore = Firebase.firestore
) : ArticleRepository {

    /* Featured Article */
    override suspend fun getFeaturedArticle(
        year: String,
        month: String,
        day: String,
        language: String
    ): Flow<Result<List<Tfa>>> {
        return flow {
            try {
                val response = api.getFeaturedArticle(year, month, day, language)
                emit(Result.Success(listOf(response.tfa)))
            } catch (e: IOException) {
                emit(Result.Error(message = "Network error: ${e.localizedMessage}"))
            } catch (e: HttpException) {
                emit(Result.Error(message = "HTTP error: ${e.code()}"))
            } catch (e: Exception) {
                emit(Result.Error(message = "Unexpected error: ${e.localizedMessage}"))
            }
        }
    }

    /* Wikipedia Search */
    override suspend fun searchPages(
        query: String,
        language: String
    ): Flow<Result<List<WikiPage>>> {
        return flow {
            try {
                if (query.isBlank()) {
                    emit(Result.Success(emptyList()))
                    return@flow
                }

                val response = api.searchPages(
                    language = language,
                    query = query,
                    limit = 10
                )

                emit(Result.Success(response.pages ?: emptyList()))
            } catch (e: IOException) {
                emit(Result.Error(message = "Please check your internet connection"))
            } catch (e: HttpException) {
                emit(Result.Error(message = when (e.code()) {
                    404 -> "Wikipedia service unavailable"
                    429 -> "Too many requests - try again later"
                    else -> "Search failed (${e.code()})"
                }))
            } catch (e: Exception) {
                emit(Result.Error(message = "Search error: ${e.localizedMessage}"))
            }
        }
    }


    override suspend fun createPostArticle(article: PostArticle): Flow<Result<String>> = flow {
        try {
            println("DEBUG: Creating post with title: ${article.title}")
            val document = firestore.collection("userArticles").document()
            // Create a map without the 'id' field
            val articleData = mapOf(
                "authorId" to article.authorId,
                "authorEmail" to article.authorEmail,
                "title" to article.title,
                "content" to article.content,
                "createdAt" to article.createdAt
            )
            document.set(articleData).await()
            println("DEBUG: Post created with ID: ${document.id}")
            emit(Result.Success(document.id))
        } catch (e: Exception) {
            println("DEBUG: Error creating post: ${e.message}")
            emit(Result.Error(message = "Failed to create post: ${e.message}"))
        }
    }


    override suspend fun getUserArticles(userId: String): Flow<Result<List<PostArticle>>> = flow {
        try {
            val currentUser = Firebase.auth.currentUser
            if (currentUser?.email == null) {
                emit(Result.Success(emptyList()))
                return@flow
            }

            println("🔍 Querying posts for email: ${currentUser.email}")

            val snapshot = firestore.collection("userArticles")
                .whereEqualTo("authorEmail", currentUser.email)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val articles = snapshot.documents.mapNotNull { doc ->
                doc.toObject(PostArticle::class.java)?.copy(id = doc.id)
            }

            println("✅ Found ${articles.size} posts")
            emit(Result.Success(articles))
        } catch (e: Exception) {
            println("❌ Error fetching posts: ${e.message}")
            emit(Result.Error(message = "Failed to fetch posts: ${e.message}"))
        }
    }

    override suspend fun getAllArticles(): Flow<Result<List<PostArticle>>> = flow {
        try {
            println("🔍 Querying all posts")

            val snapshot = firestore.collection("userArticles")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val articles = snapshot.documents.mapNotNull { doc ->
                doc.toObject(PostArticle::class.java)?.copy(id = doc.id)
            }

            println("✅ Found ${articles.size} posts")
            emit(Result.Success(articles))
        } catch (e: Exception) {
            println("❌ Error fetching all posts: ${e.message}")
            emit(Result.Error(message = "Failed to fetch posts: ${e.message}"))
        }
    }
}






