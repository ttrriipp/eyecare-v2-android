package com.eyecare.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "appointments")
data class AppointmentEntity(
    @PrimaryKey val id: Int,
    val visitReason: String,
    val status: String,
    val scheduledAt: String,
    val contactNotes: String?,
    val staffNotes: String?,
    val assignedStaffJson: String?, // JSON: {id, name} or null
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: Int,
    val orderNumber: String,
    val appointmentId: Int?,
    val billingId: Int?,
    val isNonPrescription: Boolean,
    val status: String,
    val subtotal: String,
    val totalAmount: String,
    val itemsJson: String, // JSON array of order items
    val createdAt: String,
)

@Entity(tableName = "prescriptions")
data class PrescriptionEntity(
    @PrimaryKey val id: Int,
    val appointmentId: Int?,
    val odSphere: String?,
    val odCylinder: String?,
    val odAxis: String?,
    val odAdd: String?,
    val osSphere: String?,
    val osCylinder: String?,
    val osAxis: String?,
    val osAdd: String?,
    val pd: String?,
    val prescribedAt: String,
    val expiresAt: String?,
    val notes: String?,
)

@Entity(tableName = "billings")
data class BillingEntity(
    @PrimaryKey val id: Int,
    val billingNumber: String,
    val orNumber: String?,
    val status: String,
    val subtotal: String,
    val discountAmount: String,
    val totalAmount: String,
    val amountPaid: String,
    val balanceDue: String,
    val issuedAt: String?,
    val createdAt: String,
    val itemsJson: String, // JSON array of billing items
    val paymentsJson: String, // JSON array of payments
)

@Entity(tableName = "cached_user")
data class UserEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val email: String,
    val phone: String?,
    val role: String,
)

@Entity(tableName = "visit_reasons")
data class VisitReasonEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val durationMinutes: Int,
)

@Entity(tableName = "brands")
data class BrandEntity(
    @PrimaryKey val id: Int,
    val name: String,
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: Int,
    val name: String,
)
