package com.example.museapp.presentation.feature.feedback

import android.util.Log
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.museapp.data.util.NetworkResult
import com.example.museapp.domain.usecase.SendFeedbackUseCase
import com.example.museapp.presentation.feature.feedback.ui.FeedbackUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "FEEDBACK_VM"

@HiltViewModel
class FeedbackViewModel @Inject constructor(
    private val sendFeedbackUseCase: SendFeedbackUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedbackUiState())
    val uiState: StateFlow<FeedbackUiState> = _uiState.asStateFlow()

    // Emits short user-visible messages (snackbars). Nulls are ignored by UI.
    private val _effect = MutableSharedFlow<String?>()
    val effect: SharedFlow<String?> = _effect.asSharedFlow()

    // Called by Compose OutlinedTextField (TextFieldValue)
    fun onFeedbackTextChanged(value: TextFieldValue) {
        _uiState.value = _uiState.value.copy(feedbackText = value)
    }

    // Convenience overload if caller provides a plain String
    fun onFeedbackTextChanged(value: String) {
        _uiState.value = _uiState.value.copy(feedbackText = TextFieldValue(value))
    }

    // Public submit action — emits validation or result messages via _effect
    fun submit() {
        val text = _uiState.value.feedbackText.text.trim()
        Log.d(TAG, "submit() called with text length=${text.length}")

        // Validation: if empty, emit snackbar message and return
        if (text.isEmpty()) {
            viewModelScope.launch {
                _effect.emit("Please enter feedback")
            }
            return
        }

        // Proceed with network call
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                when (val res = sendFeedbackUseCase.invoke(text)) {
                    is NetworkResult.Success -> {
                        Log.d(TAG, "feedback success: ${res.data}")
                        _uiState.value = _uiState.value.copy(isLoading = false, success = true, feedbackText = TextFieldValue(""))
                        // Emit the same success message used across app so UI uses consistent snackbar wording
                        _effect.emit("Feedback submitted. Thanks!")
                    }
                    is NetworkResult.Error -> {
                        Log.d(TAG, "feedback error: ${res.message}")
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        _effect.emit(res.message ?: "Failed to send feedback")
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "submit failed", t)
                _uiState.value = _uiState.value.copy(isLoading = false)
                _effect.emit("Failed to send feedback: ${t.message ?: "Unknown error"}")
            }
        }
    }
}
