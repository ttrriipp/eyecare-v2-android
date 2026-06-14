package com.eyecare.app.domain.repository

import com.eyecare.app.domain.model.Billing

interface BillingRepository {
    suspend fun getBilling(id: Int): Result<Billing>
}
