package com.eyecare.app.presentation.accessories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.OrderRequestFilter
import com.eyecare.app.domain.model.OrderRequestStatus
import com.eyecare.app.domain.repository.AccessoryOrderRequestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AccessoryCheckoutEligibility {
    CHECKING,
    ALLOWED,
    PENDING_REQUEST,
    UNAVAILABLE,
}

@HiltViewModel
class AccessoryCheckoutGateViewModel @Inject constructor(
    private val repository: AccessoryOrderRequestRepository,
) : ViewModel() {

    private val _eligibility = MutableStateFlow(AccessoryCheckoutEligibility.CHECKING)
    val eligibility: StateFlow<AccessoryCheckoutEligibility> = _eligibility.asStateFlow()

    private var refreshJob: Job? = null

    fun refresh() {
        refreshJob?.cancel()
        _eligibility.value = AccessoryCheckoutEligibility.CHECKING
        refreshJob = viewModelScope.launch {
            try {
                var page = 1
                while (true) {
                    val result = repository.getRequests(OrderRequestFilter.CURRENT, page)
                    val requests = result.getOrElse {
                        _eligibility.value = AccessoryCheckoutEligibility.UNAVAILABLE
                        return@launch
                    }

                    if (requests.data.any { it.status == OrderRequestStatus.PENDING }) {
                        _eligibility.value = AccessoryCheckoutEligibility.PENDING_REQUEST
                        return@launch
                    }

                    if (page >= requests.lastPage) {
                        _eligibility.value = AccessoryCheckoutEligibility.ALLOWED
                        return@launch
                    }
                    page++
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _eligibility.value = AccessoryCheckoutEligibility.UNAVAILABLE
            }
        }
    }
}
