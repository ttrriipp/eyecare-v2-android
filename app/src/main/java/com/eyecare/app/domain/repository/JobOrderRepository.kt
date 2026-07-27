package com.eyecare.app.domain.repository

import com.eyecare.app.domain.model.JobOrder

interface JobOrderRepository {
    suspend fun getJobOrders(page: Int = 1): Result<PaginatedResult<JobOrder>>
    suspend fun getJobOrder(id: Int): Result<JobOrder>
}
