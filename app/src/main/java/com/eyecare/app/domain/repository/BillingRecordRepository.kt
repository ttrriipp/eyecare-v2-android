package com.eyecare.app.domain.repository

import com.eyecare.app.domain.model.BillingRecord

interface BillingRecordRepository {
    suspend fun getBillingRecords(page: Int = 1): Result<PaginatedResult<BillingRecord>>
    suspend fun getBillingRecord(id: Int): Result<BillingRecord>
}
