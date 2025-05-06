package com.example.wikinow.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.wikinow.data.ArticleRepositoryImpl
import com.example.wikinow.data.model.PostArticle
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: String?,
    navController: NavController,
    isReadingMode: MutableState<Boolean>
) {
    var article by remember { mutableStateOf<PostArticle?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(postId) {
        if (postId == null) {
            error = "Invalid article ID"
            isLoading = false
            return@LaunchedEffect
        }

        try {
            val snapshot = Firebase.firestore
                .collection("userArticles")
                .document(postId)
                .get()
                .await()

            if (snapshot.exists()) {
                article = snapshot.toObject(PostArticle::class.java)?.copy(id = postId)
            } else {
                error = "Article not found"
            }
        } catch (e: Exception) {
            error = "Error loading article: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(article?.title ?: "Article") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                error != null -> {
                    Text(
                        text = error ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
                article != null -> {
                    ArticleContent(article!!)
                }
            }
        }
    }
}

@Composable
private fun ArticleContent(article: PostArticle) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Article Title
        Text(
            text = article.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // Author and Date
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "By: ${article.authorEmail}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )

            article.createdAt?.toString()?.substring(0, 10)?.let { date ->
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // Article Content
        Text(
            text = article.content,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}