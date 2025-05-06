package com.example.wikinow.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.wikinow.data.model.PostArticle
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import androidx.compose.material3.ExtendedFloatingActionButton
import kotlin.compareTo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(navController: NavController) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    val context = LocalContext.current
    val db = Firebase.firestore
    val auth = Firebase.auth
    val coroutineScope = rememberCoroutineScope()
    var isCheckingContent by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Create Post",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = {
                    if (isCheckingContent) Text("Checking...")
                    else Text("Publish")
                },
                icon = {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Post")
                },
                onClick = {
                    if (isCheckingContent) return@ExtendedFloatingActionButton

                    val currentUser = auth.currentUser
                    if (currentUser != null) {
                        if (content.length < 300) {
                            Toast.makeText(
                                context,
                                "Please write at least 300 characters for reliable AI detection",
                                Toast.LENGTH_LONG
                            ).show()
                            return@ExtendedFloatingActionButton
                        }

                        isCheckingContent = true

                        coroutineScope.launch {
                            try {
                                val isHumanWritten = checkIsHumanWritten(content)

                                withContext(Dispatchers.Main) {
                                    if (isHumanWritten) {
                                        Toast.makeText(
                                            context,
                                            "Content verified as human-written!",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        val article = PostArticle(
                                            authorId = currentUser.uid,
                                            authorEmail = currentUser.email ?: "",
                                            title = title,
                                            content = content
                                        )
                                        db.collection("userArticles")
                                            .add(article)
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "Article posted!", Toast.LENGTH_SHORT).show()
                                                navController.popBackStack()
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "AI-generated content detected. Please write the article yourself.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                    isCheckingContent = false
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "Error checking content: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    isCheckingContent = false
                                }
                            }
                        }
                    }
                },
                expanded = title.isNotBlank() && content.isNotBlank(),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(vertical = 24.dp, horizontal = 16.dp)
            ) {
                Text(
                    text = "Come share your thoughts with the WikiNow community!",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Form Card
            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Post Title") },
                        placeholder = { Text("e.g. How to get an IP") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Post Content") },
                        placeholder = { Text("Share your knowledge or ask a question...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp),
                        maxLines = Int.MAX_VALUE
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp)) // Padding for FAB
        }
    }
}

suspend fun checkIsHumanWritten(content: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            // TODO: Store this token securely in BuildConfig or encrypted preferences
            val apiToken = "8HpoJTproBlRQPCGk0JC1xByEcu2MbYhNUWU4nFx5bae19f1"

            val client = OkHttpClient()

            // Escape special characters in content
            val escapedContent = content
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")

            val json = """
                {
                    "text": "$escapedContent",
                    "version": "latest",
                    "sentences": false,
                    "language": "auto"
                }
            """.trimIndent()

            val requestBody = json.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("https://api.gowinston.ai/v2/ai-content-detection")
                .header("Authorization", "Bearer $apiToken")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val jsonObject = JSONObject(responseBody)
                val score = jsonObject.optInt("score", 0)

                // Score of 70+ indicates likely human-written content
                return@withContext score >= 70
            }

            // If API call fails, default to blocking the post for safety
            return@withContext false
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }
}