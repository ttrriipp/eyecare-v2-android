package com.eyecare.app.data.local

import com.eyecare.app.data.local.dao.PendingOperationDao
import com.eyecare.app.data.local.entity.PendingOperationEntity
import com.eyecare.app.data.remote.api.AppointmentApiService
import com.eyecare.app.data.remote.api.AuthApiService
import com.eyecare.app.data.remote.api.ConversationApiService
import com.eyecare.app.data.remote.api.FeedbackApiService
import com.eyecare.app.data.remote.api.OrderApiService
import com.eyecare.app.data.remote.dto.AuthDtos
import com.eyecare.app.data.remote.dto.FeedbackDtos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val MAX_RETRIES = 3

@Singleton
class SyncManager @Inject constructor(
    private val networkMonitor: NetworkMonitor,
    private val pendingDao: PendingOperationDao,
    private val appointmentApi: AppointmentApiService,
    private val orderApi: OrderApiService,
    private val authApi: AuthApiService,
    private val feedbackApi: FeedbackApiService,
    private val conversationApi: ConversationApiService,
    private val json: Json,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val pendingCount: Flow<Int> = pendingDao.getPendingCountFlow()

    init {
        // Observe connectivity changes and sync when online
        scope.launch {
            networkMonitor.isOnline.collect { online ->
                if (online) processQueue()
            }
        }
    }

    suspend fun enqueue(type: String, payload: String) {
        pendingDao.insert(PendingOperationEntity(type = type, payload = payload))
        // Try immediately if online
        if (networkMonitor.isOnline.value) processQueue()
    }

    suspend fun retryFailed(id: Long) {
        val op = pendingDao.getAll().find { it.id == id } ?: return
        pendingDao.update(op.copy(status = "pending", retryCount = 0, errorMessage = null))
        if (networkMonitor.isOnline.value) processQueue()
    }

    suspend fun discard(id: Long) {
        pendingDao.delete(id)
    }

    suspend fun getPendingOperations(): List<PendingOperationEntity> = pendingDao.getAll()

    private suspend fun processQueue() {
        val pending = pendingDao.getPending()
        for (op in pending) {
            val success = executeOperation(op)
            if (success) {
                pendingDao.delete(op.id)
            } else {
                val newRetry = op.retryCount + 1
                if (newRetry >= MAX_RETRIES) {
                    pendingDao.update(op.copy(status = "failed", retryCount = newRetry))
                } else {
                    pendingDao.update(op.copy(retryCount = newRetry))
                }
            }
        }
    }

    private suspend fun executeOperation(op: PendingOperationEntity): Boolean {
        return try {
            when (op.type) {
                "cancel_appointment" -> {
                    val payload = json.decodeFromString<IdPayload>(op.payload)
                    appointmentApi.cancelAppointment(payload.id)
                    true
                }
                "cancel_order" -> {
                    val payload = json.decodeFromString<IdPayload>(op.payload)
                    orderApi.cancelOrder(payload.id)
                    true
                }
                "update_profile" -> {
                    val payload = json.decodeFromString<ProfilePayload>(op.payload)
                    authApi.updateUser(AuthDtos.UpdateUserRequest(payload.name, payload.email, payload.phone))
                    true
                }
                "submit_feedback" -> {
                    val payload = json.decodeFromString<FeedbackPayload>(op.payload)
                    feedbackApi.submitFeedback(
                        FeedbackDtos.SubmitFeedbackRequest(payload.appointmentId, payload.orderId, payload.rating, payload.comment),
                    )
                    true
                }
                "mark_read" -> {
                    val payload = json.decodeFromString<IdPayload>(op.payload)
                    conversationApi.markMessagesRead(payload.id)
                    true
                }
                else -> {
                    pendingDao.update(op.copy(status = "failed", errorMessage = "Unknown operation type"))
                    false
                }
            }
        } catch (e: Exception) {
            pendingDao.update(op.copy(errorMessage = e.message))
            false
        }
    }
}

// Payload data classes for serialization
@Serializable
data class IdPayload(val id: Int)

@Serializable
data class ProfilePayload(val name: String, val email: String, val phone: String?)

@Serializable
data class FeedbackPayload(
    val appointmentId: Int? = null,
    val orderId: Int? = null,
    val rating: Int,
    val comment: String? = null,
)
