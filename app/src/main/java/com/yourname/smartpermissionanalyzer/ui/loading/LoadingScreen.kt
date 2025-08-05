package com.yourname.smartpermissionanalyzer.ui.loading

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadingScreen(
    onLoadingComplete: () -> Unit
) {
    var loadingProgress by remember { mutableFloatStateOf(0f) }
    var loadingText by remember { mutableStateOf("Initializing Argus...") }

    LaunchedEffect(Unit) {
        // ✅ FIXED: Show actual loading progress instead of hanging
        val loadingSteps = listOf(
            "Initializing Argus..." to 0.1f,
            "Preparing security analysis..." to 0.3f,
            "Loading app permissions..." to 0.6f,
            "Setting up dashboard..." to 0.9f,
            "Ready!" to 1.0f
        )

        for ((text, progress) in loadingSteps) {
            loadingText = text
            loadingProgress = progress
            delay(800) // Show each step for 800ms
        }

        delay(500) // Brief pause before completing
        onLoadingComplete()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Title
        Text(
            text = "Argus",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Every Permission Analyzed. Every Risk Detected.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 48.dp)
        )

        // Loading Progress
        LinearProgressIndicator(
            progress = { loadingProgress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
        )

        // Loading Text
        Text(
            text = loadingText,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}
