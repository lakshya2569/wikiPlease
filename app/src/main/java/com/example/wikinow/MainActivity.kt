package com.example.wikinow

import ArticleViewModel
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.example.wikinow.data.ArticleRepositoryImpl
import com.example.wikinow.data.model.Tfa
import com.example.wikinow.data.model.WikiPage

import com.example.wikinow.presentation.ArticleViewModelFactory
import com.example.wikinow.screens.*
import com.example.wikinow.ui.theme.WikinowTheme
import com.example.wikinow.utils.rememberVoiceSearch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(
                this,
                "Microphone permission is required for voice search",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Firebase.initialize(this)
        enableEdgeToEdge()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
        setContent {
            val isReadingMode = remember { mutableStateOf(false) }
            WikinowTheme(readingMode = isReadingMode.value) {
                val navController = rememberNavController()
                val auth = Firebase.auth

                // Navigation graph setup
                AppNavigation(
                    navController = navController,
                    isUserLoggedIn = auth.currentUser != null,
                    isReadingMode = isReadingMode
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation(
    navController: NavHostController,
    isUserLoggedIn: Boolean,
    isReadingMode: MutableState<Boolean>
) {
    // Handle initial navigation based on auth state
    LaunchedEffect(isUserLoggedIn) {
        if (isUserLoggedIn) {
            navController.navigate("main") {
                popUpTo(navController.graph.startDestinationId)
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (isUserLoggedIn) "main" else "login"
    ) {
        composable("login") {
            LoginScreen(
                navController = navController,
                viewModel = viewModel(),

            )
        }
        composable("signup") {
            SignupScreen(
                navController = navController,
                viewModel = viewModel(),
                isReadingMode = isReadingMode
            )
        }
        composable("main") {
            MainScreen(
                navController = navController,
                isReadingMode = isReadingMode
            )
        }
        composable(
            "article_detail/{articleId}/{title}",
            arguments = listOf(
                navArgument("articleId") { type = androidx.navigation.NavType.LongType },
                navArgument("title") { type = androidx.navigation.NavType.StringType }
            )
        ) { backStackEntry ->
            val articleId = backStackEntry.arguments?.getLong("articleId")
            val title = backStackEntry.arguments?.getString("title")
            ArticleDetailScreen(
                articleId = articleId?.toString(),
                title = title,
                navController = navController,
                isReadingMode = isReadingMode
            )
        }
        composable("create_post") {
            CreatePostScreen(
                navController = navController,

            )
        }
        composable("my_posts") {
            MyPostsScreen(
                navController = navController,

            )
        }

        composable(
            "post_detail/{postId}",
            arguments = listOf(
                navArgument("postId") { type = androidx.navigation.NavType.StringType }
            )
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId")
            PostDetailScreen(
                postId = postId,
                navController = navController,
                isReadingMode = isReadingMode
            )
        }
    }
}

@SuppressLint("ServiceCast")
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class) // Remove this if Scaffold is stable in your Compose version
@Composable
fun MainScreen(
    navController: NavHostController,
    isReadingMode: MutableState<Boolean>,
    authViewModel: AuthViewModel = viewModel(),
    auth: FirebaseAuth = Firebase.auth
) {
    // ViewModel setup
    val repository = ArticleRepositoryImpl(RetrofitInstance.api)
    val articleViewModel: ArticleViewModel = viewModel(
        factory = ArticleViewModelFactory(repository)
    )

    // State declarations
    val articles by articleViewModel.article.collectAsState()
    val searchResults by articleViewModel.searchResults.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val voiceSearch = rememberVoiceSearch()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Debounced text search
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            isSearching = true
            delay(300)
            articleViewModel.searchPages(searchQuery.trim())
            isSearching = false
        } else {
            articleViewModel.clearSearchResults()
        }
    }

    // Handle voice search results
    LaunchedEffect(voiceSearch.voiceResult.value) {
        voiceSearch.voiceResult.value?.let { result ->
            when (result) {
                "PERMISSION_NEEDED" -> {
                    Toast.makeText(
                        context,
                        "Microphone permission required",
                        Toast.LENGTH_LONG
                    ).show()
                }
                else -> {
                    searchQuery = result
                    articleViewModel.searchPages(result)
                }
            }
            voiceSearch.voiceResult.value = null
        }
    }

    // Error handler
    LaunchedEffect(Unit) {
        articleViewModel.showErrorToastChannel.collectLatest { show ->
            if (show) {
                Toast.makeText(context, "Failed to load data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WikiNow") },
                actions = {
                    IconButton(onClick = { isReadingMode.value = !isReadingMode.value }) {
                        Icon(
                            imageVector = if (isReadingMode.value) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isReadingMode.value) "Switch to Light Mode" else "Switch to Reading Mode"
                        )
                    }
                    IconButton(
                        onClick = {
                            Firebase.auth.signOut()
                            navController.navigate("login") {
                                popUpTo("main") { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout"
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate("create_post") },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Post"
                        )
                    },
                    label = { Text("New Post") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate("my_posts") },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = "My Posts"
                        )
                    },
                    label = { Text("My Posts") }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Persistent Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search Wikipedia...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Icon"
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            articleViewModel.clearSearchResults()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Search"
                            )
                        }
                    } else {
                        IconButton(onClick = {
                            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                            voiceSearch.startVoiceSearch()
                        }) {
                            Icon(
                                imageVector = Icons.Default.KeyboardVoice,
                                contentDescription = "Voice Search",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        articleViewModel.searchPages(searchQuery)
                        keyboardController?.hide()
                    }
                ),
                singleLine = true
            )

            // Content: Search results or featured article
            if (searchQuery.isNotBlank() && !isSearching) {
                if (searchResults.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No results found", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        itemsIndexed(searchResults) { _, page ->
                            WikiSearchResultItem(page) {
                                navController.navigate("article_detail/${page.id}/${page.title}")
                            }
                        }
                    }
                }
            } else if (isSearching) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                FeaturedArticleView(articles)
            }
        }
    }
}

@Composable
private fun FeaturedArticleView(articles: List<Tfa>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (articles.isEmpty()) {
            Spacer(modifier = Modifier.height(40.dp))
            CircularProgressIndicator()
        } else {
            val tfa = articles.first()

            // Safely unwrap all possibly-null fields
            val titleText = tfa.title ?: "No title available"
            val descText = tfa.description ?: ""
            val extractText = tfa.extract ?: ""

            Text(
                text = titleText,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(8.dp)
            )

            tfa.thumbnail?.source?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = titleText,
                    modifier = Modifier
                        .size(200.dp)
                        .clip(MaterialTheme.shapes.medium)
                )
            }

            if (descText.isNotEmpty()) {
                Text(
                    text = descText,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(4.dp)
                )
            }

            if (extractText.isNotEmpty()) {
                Text(
                    text = extractText,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun WikiSearchResultItem(page: WikiPage, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            page.thumbnail?.url?.let { url ->
                AsyncImage(
                    model = "https:$url",
                    contentDescription = page.title,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            // Text content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = page.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = page.excerpt.stripHtmlTags(),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                page.description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

private fun String.stripHtmlTags(): String = replace(Regex("<[^>]+>"), "")