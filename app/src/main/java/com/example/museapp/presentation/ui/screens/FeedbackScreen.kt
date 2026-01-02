package com.example.museapp.presentation.feature.feedback

import android.util.Log
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.museapp.presentation.feature.feedback.ui.FeedbackUiState
import com.example.museapp.presentation.ui.components.appTextFieldColors
import com.example.museapp.ui.theme.AppTypography
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = "FEEDBACK_UI"

/**
 * Feedback screen that shows snackbars using the Scaffold's SnackbarHostState.
 * It collects `viewModel.effect` SharedFlow and displays messages (validation & success).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    viewModel: FeedbackViewModel = hiltViewModel(),
    onBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val effectFlow = viewModel.effect

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val backDispatcherOwner = LocalOnBackPressedDispatcherOwner.current

    // Collect effects and show snackbars. collectLatest ensures only the latest inflight is shown.
    LaunchedEffect(effectFlow) {
        effectFlow.collectLatest { message ->
            message?.let {
                Log.d(TAG, "Effect received (will show snackbar): $it")
                // show snackbar with short duration to match typical validation/snackbars in app
                val result = snackbarHostState.showSnackbar(
                    message = it,
                    duration = SnackbarDuration.Short
                )
                // Optionally handle dismiss/perform action on action (not used here)
                when (result) {
                    SnackbarResult.Dismissed -> Log.d(TAG, "Snackbar dismissed")
                    SnackbarResult.ActionPerformed -> Log.d(TAG, "Snackbar action performed")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Send Feedback", style = AppTypography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (onBack != null) onBack() else backDispatcherOwner?.onBackPressedDispatcher?.onBackPressed()
                    }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Tell us what you love about the app, or what we could be doing better.",
                    style = AppTypography.bodyLarge
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(text = "Enter feedback", style = AppTypography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = uiState.feedbackText,
                    onValueChange = { viewModel.onFeedbackTextChanged(it) },
                    label = { Text("Feedback", style = AppTypography.titleSmall) },
                    placeholder = { Text(text = "Write your feedback here", style = AppTypography.titleSmall) },
                    singleLine = false,
                    colors = appTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 8,
                    textStyle = AppTypography.bodyLarge
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    } else {
                        Button(onClick = {
                            // Log then call submit; ViewModel will emit validation or success messages
                            Log.d(TAG, "Submit clicked length=${uiState.feedbackText.text.length}")
                            viewModel.submit()
                        }) {
                            Text(text = "Submit", style = AppTypography.titleSmall)
                        }
                    }
                }
            }
        }
    }
}
