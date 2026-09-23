package com.eyecare.app.presentation.eyewear

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.ProductRatingAttachment
import com.eyecare.app.domain.model.RatingResult
import com.eyecare.app.domain.repository.OpticalOrderRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface FrameRatingUiState {
    data object Idle : FrameRatingUiState
    data object Submitting : FrameRatingUiState
    data class Success(val result: RatingResult) : FrameRatingUiState
    data class Error(val message: String, val fieldErrors: Map<String, List<String>>? = null) : FrameRatingUiState
}

@HiltViewModel(assistedFactory = FrameRatingViewModel.Factory::class)
class FrameRatingViewModel @AssistedInject constructor(
    private val repository: OpticalOrderRepository,
    @Assisted("orderItemId") private val orderItemId: Int,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("orderItemId") orderItemId: Int,
        ): FrameRatingViewModel
    }

    private val _uiState = MutableStateFlow<FrameRatingUiState>(FrameRatingUiState.Idle)
    val uiState: StateFlow<FrameRatingUiState> = _uiState.asStateFlow()

    fun submitRating(
        rating: Int,
        comment: String?,
        publicDisplayConsent: Boolean = false,
    ) {
        submitRating(
            rating = rating,
            comment = comment,
            publicDisplayConsent = publicDisplayConsent,
            attachment = null,
            publicAttachmentConsent = false,
        )
    }

    fun submitRatingWithAttachment(
        rating: Int,
        comment: String?,
        publicDisplayConsent: Boolean,
        attachment: ProductRatingAttachment?,
        publicAttachmentConsent: Boolean,
    ) {
        submitRating(
            rating = rating,
            comment = comment,
            publicDisplayConsent = publicDisplayConsent,
            attachment = attachment,
            publicAttachmentConsent = publicAttachmentConsent,
        )
    }

    private fun submitRating(
        rating: Int,
        comment: String?,
        publicDisplayConsent: Boolean,
        attachment: ProductRatingAttachment?,
        publicAttachmentConsent: Boolean,
    ) {
        if (rating < 1 || rating > 5) {
            _uiState.value = FrameRatingUiState.Error("Rating must be between 1 and 5")
            return
        }
        if (comment != null && comment.length > 1000) {
            _uiState.value = FrameRatingUiState.Error("Comment must be 1000 characters or less")
            return
        }
        val attachmentError = attachment?.let {
            sequenceOf(
                PaymentProofInspector.validateMimeType(it.mimeType),
                PaymentProofInspector.validateFileSize(it.imageFile.length()),
                PaymentProofInspector.validateDimensions(it.width, it.height),
            ).filterNotNull().firstOrNull()
        }
        if (attachmentError != null) {
            _uiState.value = FrameRatingUiState.Error(attachmentError)
            return
        }

        _uiState.value = FrameRatingUiState.Submitting
        viewModelScope.launch {
            repository.rateItem(
                itemId = orderItemId,
                rating = rating,
                comment = comment?.takeIf { it.isNotBlank() },
                publicDisplayConsent = publicDisplayConsent && !comment.isNullOrBlank(),
                attachment = attachment,
                publicAttachmentConsent = publicAttachmentConsent,
            ).fold(
                onSuccess = { result ->
                    _uiState.value = FrameRatingUiState.Success(result)
                },
                onFailure = { error ->
                    _uiState.value = FrameRatingUiState.Error(
                        message = error.message ?: "Failed to submit rating",
                    )
                },
            )
        }
    }

    fun reset() {
        _uiState.value = FrameRatingUiState.Idle
    }
}
