package com.eyecare.app.data.repository

import com.eyecare.app.data.local.dao.BillingDao
import com.eyecare.app.data.local.entity.BillingEntity
import com.eyecare.app.data.remote.api.BillingApiService
import com.eyecare.app.data.remote.dto.BillingDtos
import com.eyecare.app.domain.model.Billing
import com.eyecare.app.domain.model.BillingItem
import com.eyecare.app.domain.model.BillingStatus
import com.eyecare.app.domain.model.Payment
import com.eyecare.app.domain.repository.BillingRepository
import kotlinx.serialization.json.Json
import java.io.InputStream
import javax.inject.Inject

class BillingRepositoryImpl @Inject constructor(
    private val api: BillingApiService,
    private val json: Json,
    private val billingDao: BillingDao,
) : BillingRepository {

    override suspend fun getBilling(id: Int): Result<Billing> {
        return try {
            val dto = api.getBilling(id).data
            billingDao.insert(dto.toEntity())
            Result.success(dto.toDomain())
        } catch (e: Exception) {
            val cached = billingDao.getById(id)
            if (cached != null) {
                Result.success(cached.toDomain())
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun downloadPdf(id: Int): Result<InputStream> = runCatching {
        api.downloadBillingPdf(id).byteStream()
    }

    // ── DTO → Domain ──

    private fun BillingDtos.BillingDto.toDomain() = Billing(
        id = id,
        billingNumber = billingNumber,
        orNumber = orNumber,
        status = BillingStatus.from(status),
        subtotal = subtotal,
        discountAmount = discountAmount,
        totalAmount = totalAmount,
        amountPaid = amountPaid,
        balanceDue = balanceDue,
        issuedAt = issuedAt,
        createdAt = createdAt,
        items = items.map { it.toDomain() },
        payments = payments.map { p ->
            Payment(p.id, p.amount, p.status, p.method, p.referenceNumber, p.paidAt)
        },
    )

    private fun BillingDtos.BillingItemDto.toDomain() = BillingItem(
        id = id,
        type = type,
        description = description,
        quantity = quantity,
        unitPrice = unitPrice,
        amount = amount,
    )

    // ── DTO → Entity ──

    private fun BillingDtos.BillingDto.toEntity() = BillingEntity(
        id = id,
        billingNumber = billingNumber,
        orNumber = orNumber,
        status = status,
        subtotal = subtotal,
        discountAmount = discountAmount,
        totalAmount = totalAmount,
        amountPaid = amountPaid,
        balanceDue = balanceDue,
        issuedAt = issuedAt,
        createdAt = createdAt,
        itemsJson = json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(BillingDtos.BillingItemDto.serializer()),
            items,
        ),
        paymentsJson = json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(BillingDtos.PaymentDto.serializer()),
            payments,
        ),
    )

    // ── Entity → Domain ──

    private fun BillingEntity.toDomain(): Billing {
        val items = json.decodeFromString(
            kotlinx.serialization.builtins.ListSerializer(BillingDtos.BillingItemDto.serializer()),
            itemsJson,
        )
        val payments = json.decodeFromString(
            kotlinx.serialization.builtins.ListSerializer(BillingDtos.PaymentDto.serializer()),
            paymentsJson,
        )
        return Billing(
            id = id,
            billingNumber = billingNumber,
            orNumber = orNumber,
            status = BillingStatus.from(status),
            subtotal = subtotal,
            discountAmount = discountAmount,
            totalAmount = totalAmount,
            amountPaid = amountPaid,
            balanceDue = balanceDue,
            issuedAt = issuedAt,
            createdAt = createdAt,
            items = items.map { it.toDomain() },
            payments = payments.map { p ->
                Payment(p.id, p.amount, p.status, p.method, p.referenceNumber, p.paidAt)
            },
        )
    }
}
