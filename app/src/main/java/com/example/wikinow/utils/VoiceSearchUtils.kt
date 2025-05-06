package com.example.wikinow.utils

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.util.Locale

@Composable
fun rememberVoiceSearch(): VoiceSearchState {
    val context = LocalContext.current
    val voiceResult = remember { mutableStateOf<String?>(null) }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spokenText = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        voiceResult.value = spokenText
    }

    return remember {
        VoiceSearchState(
            startVoiceSearch = {
                if (ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.RECORD_AUDIO
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                        )
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to search Wikipedia")
                    }
                    voiceLauncher.launch(intent)
                } else {
                    // Handle permission denial (will be handled by MainActivity)
                    voiceResult.value = "PERMISSION_NEEDED"
                }
            },
            voiceResult = voiceResult
        )
    }
}

class VoiceSearchState(
    val startVoiceSearch: () -> Unit,
    val voiceResult: androidx.compose.runtime.MutableState<String?>
)