import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wikinow.data.ArticleRepository
import com.example.wikinow.data.model.Tfa
import com.example.wikinow.data.Result
import com.example.wikinow.data.model.PostArticle
import com.example.wikinow.data.model.WikiPage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.time.LocalDate

import java.util.Locale

@SuppressLint("DefaultLocale")
@RequiresApi(Build.VERSION_CODES.O)
class ArticleViewModel(
    private val articleRepository: ArticleRepository
) : ViewModel() {
    private val _article = MutableStateFlow<List<Tfa>>(emptyList())
    val article = _article.asStateFlow()

    private val _showErrorToastChannel = Channel<Boolean>()
    val showErrorToastChannel = _showErrorToastChannel.receiveAsFlow()

    private val _searchResults = MutableStateFlow<List<WikiPage>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _userArticles = MutableStateFlow<List<PostArticle>>(emptyList())
    val userArticles: StateFlow<List<PostArticle>> = _userArticles.asStateFlow()

    private val _postArticleState = MutableStateFlow<Result<String>?>(null)
    val postArticleState = _postArticleState.asStateFlow()

    private val _allArticles = MutableStateFlow<List<PostArticle>>(emptyList())
    val allArticles: StateFlow<List<PostArticle>> = _allArticles.asStateFlow()


    fun searchPages(query: String, language: String = "en") {
        viewModelScope.launch {
            articleRepository.searchPages(query, language).collect { result ->
                when (result) {
                    is Result.Success -> _searchResults.value = result.data ?: emptyList()
                    is Result.Error -> _showErrorToastChannel.send(true)
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            val today = LocalDate.now()
            val year = today.year.toString()
            val month = String.format("%02d", today.monthValue)
            val day = String.format("%02d", today.dayOfMonth)

            articleRepository.getFeaturedArticle(year, month, day, "en").collectLatest { result ->
                when (result) {
                    is Result.Error -> {
                        _showErrorToastChannel.send(true)
                    }
                    is Result.Success -> {
                        result.data?.let {
                            _article.value = it
                        }
                    }
                }
            }
        }
    }
    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }

    fun createPostArticle(title: String, content: String) {
        val currentUser = Firebase.auth.currentUser ?: return

        val article = PostArticle(
            authorId = currentUser.uid,
            authorEmail = currentUser.email ?: "",
            title = title,
            content = content
        )

        viewModelScope.launch {
            articleRepository.createPostArticle(article).collect { result ->
                _postArticleState.value = result
            }
        }
    }

    fun loadUserArticles() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                println("DEBUG: Loading user articles started")
                val userId = Firebase.auth.currentUser?.uid ?: ""
                println("DEBUG: User ID for query: $userId")
                if (userId.isEmpty()) {
                    println("DEBUG: User ID is empty, aborting query")
                    _showErrorToastChannel.send(true)
                    return@launch
                }
                withTimeout(10000L) { // 10-second timeout
                    articleRepository.getUserArticles(userId).collect { result ->
                        when (result) {
                            is Result.Success -> {
                                println("DEBUG: Successfully loaded ${result.data?.size ?: 0} articles")
                                _userArticles.value = result.data ?: emptyList()
                            }
                            is Result.Error -> {
                                println("DEBUG: Error loading articles: ${result.message}")
                                _showErrorToastChannel.send(true)
                            }
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                println("DEBUG: Loading timed out")
                _showErrorToastChannel.send(true)
            } catch (e: Exception) {
                println("DEBUG: Exception in loadUserArticles: ${e.message}")
                _showErrorToastChannel.send(true)
            } finally {
                _isLoading.value = false
                println("DEBUG: Loading user articles completed")
            }
        }
    }

    fun loadAllArticles() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                println("DEBUG: Loading all articles started")

                withTimeout(10000L) { // 10-second timeout
                    articleRepository.getAllArticles().collect { result ->
                        when (result) {
                            is Result.Success -> {
                                println("DEBUG: Successfully loaded ${result.data?.size ?: 0} articles")
                                _allArticles.value = result.data ?: emptyList()
                            }
                            is Result.Error -> {
                                println("DEBUG: Error loading articles: ${result.message}")
                                _showErrorToastChannel.send(true)
                            }
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                println("DEBUG: Loading timed out")
                _showErrorToastChannel.send(true)
            } catch (e: Exception) {
                println("DEBUG: Exception in loadAllArticles: ${e.message}")
                _showErrorToastChannel.send(true)
            } finally {
                _isLoading.value = false
                println("DEBUG: Loading all articles completed")
            }
        }
    }

}







