package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.BillingApiService
import com.eyecare.app.data.remote.dto.BillingDtos
import com.eyecare.app.domain.model.Billing
import com.eyecare.app.domain.model.BillingItem
import com.eyecare.app.domain.model.BillingStatus
import com.eyecare.app.domain.model.Payment
import com.eyecare.app.domain.repository.BillingRepository
import javax.inject.Inject

class BillingRepositoryImpl @Inject constructor(
    private val api: BillingApiService,
) : BillingRepository {

    override suspend fun getBilling(id: Int): Result<Billing> = runCatching {
        api.getBilling(id).data.toDomain()
    }

    private fun BillingDtos.BillingDto.toDomain() = Billing(
        id = id,
        billingNumber = billingNumber,
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
}
