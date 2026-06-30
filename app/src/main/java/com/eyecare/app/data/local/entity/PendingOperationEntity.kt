package com.eyecare.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_operations")
data class PendingOperationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // cancel_appointment, cancel_order, update_profile, submit_feedback, mark_read
    val payload: String, // JSON with endpoint params
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "pending", // pending, failed
    val errorMessage: String? = null,
    val retryCount: Int = 0,
)
