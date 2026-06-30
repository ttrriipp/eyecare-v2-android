package com.eyecare.app.domain.repository

import com.eyecare.app.domain.model.Billing
import java.io.InputStream

interface BillingRepository {
    suspend fun getBilling(id: Int): Result<Billing>
    suspend fun downloadPdf(id: Int): Result<InputStream>
}
