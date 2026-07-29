package com.eyecare.app.domain.repository

import com.eyecare.app.domain.model.EyewearDetail
import com.eyecare.app.domain.model.EyewearSummary

interface EyewearRepository {
    suspend fun getEyewear(filter: String = "current", page: Int = 1): Result<PaginatedResult<EyewearSummary>>
    suspend fun getEyewearDetail(key: String): Result<EyewearDetail>
}
