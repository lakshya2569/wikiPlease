package com.example.wikinow.screens

import ArticleViewModel
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.wikinow.RetrofitInstance
import com.example.wikinow.data.ArticleRepositoryImpl
import com.example.wikinow.data.model.PostArticle
import com.example.wikinow.presentation.ArticleViewModelFactory
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPostsScreen(navController: NavController,
                  ) {
    println("DEBUG: MyPostsScreen Composable loaded")
    val viewModel: ArticleViewModel = viewModel(
        factory = ArticleViewModelFactory(
            ArticleRepositoryImpl(RetrofitInstance.api)
        )
    )
    val context = LocalContext.current
    val userArticles = viewModel.userArticles.collectAsStateWithLifecycle().value
    val allArticles = viewModel.allArticles.collectAsStateWithLifecycle().value
    val isLoading = viewModel.isLoading.collectAsStateWithLifecycle().value
    val error = viewModel.showErrorToastChannel.collectAsStateWithLifecycle(initialValue = false).value

    var searchQuery by remember { mutableStateOf("") }
    var isSearchingAll by remember { mutableStateOf(false) }
    var isInSearchMode by remember { mutableStateOf(false) }

    val curruser = Firebase.auth.currentUser
    println("DEBUG: Current user email: ${curruser?.email}, UID: ${curruser?.uid}")

    LaunchedEffect(key1 = curruser?.uid, key2 = isSearchingAll) {
        println("DEBUG: LaunchedEffect - loading articles")
        if (isSearchingAll) {
            viewModel.loadAllArticles()
        } else {
            viewModel.loadUserArticles()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isInSearchMode) {
                        TextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                if (it.isNotBlank()) {
                                    isSearchingAll = true
                                }
                            },
                            placeholder = { Text("Search all articles...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = {
                                    if (searchQuery.isNotEmpty()) {
                                        searchQuery = ""
                                    } else {
                                        isInSearchMode = false
                                        isSearchingAll = false
                                    }
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        )
                    } else {
                        Text(if (isSearchingAll) "All Articles" else "My Posts")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isInSearchMode) {
                        IconButton(onClick = {
                            isInSearchMode = true
                            isSearchingAll = true
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when {
                isLoading -> {
                    println("DEBUG: UI - Showing loading indicator")
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                error -> {
                    println("DEBUG: UI - Showing error state")
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Failed to load posts. Please try again.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            if (isSearchingAll) viewModel.loadAllArticles()
                            else viewModel.loadUserArticles()
                        }) {
                            Text("Retry")
                        }
                    }
                }
                (isSearchingAll && allArticles.isEmpty()) || (!isSearchingAll && userArticles.isEmpty()) -> {
                    println("DEBUG: UI - Showing no posts found")
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isSearchingAll) {
                                if (searchQuery.isNotBlank()) "No matching articles found"
                                else "No articles found"
                            } else {
                                "No posts found for ${curruser?.email ?: "unknown user"}"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isSearchingAll) {
                                "Try a different search term"
                            } else {
                                "Try creating a new post or check your account."
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                else -> {
                    val displayedArticles = if (isSearchingAll) {
                        if (searchQuery.isBlank()) {
                            allArticles
                        } else {
                            allArticles.filter {
                                it.title.contains(searchQuery, ignoreCase = true) ||
                                        it.content.contains(searchQuery, ignoreCase = true)
                            }
                        }
                    } else {
                        userArticles
                    }

                    println("DEBUG: UI - Showing ${displayedArticles.size} articles")
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = displayedArticles,
                            key = { article -> "${article.title}-${article.createdAt}" }
                        ) { article ->
                            PostArticleItem(article = article) {
                                navController.navigate("post_detail/${article.id}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PostArticleItem(
    article: PostArticle,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "By ${article.authorEmail}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = article.content.take(150) + if (article.content.length > 150) "..." else "",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Posted on ${article.createdAt?.toString()?.substring(0, 10) ?: ""}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}