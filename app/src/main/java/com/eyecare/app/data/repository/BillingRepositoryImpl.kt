package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.BillingApiService
import com.eyecare.app.data.remote.dto.BillingDtos
import com.eyecare.app.domain.model.Billing
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
        id = id, orderId = orderId, status = BillingStatus.from(status),
        totalAmount = totalAmount, amountPaid = amountPaid, balanceDue = balanceDue,
        issuedAt = issuedAt, createdAt = createdAt,
        payments = payments.map { p ->
            Payment(p.id, p.amount, p.status, p.method, p.referenceNumber, p.paidAt)
        },
    )
}
