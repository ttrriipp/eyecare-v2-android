package com.eyecare.app.domain.repository

import com.eyecare.app.domain.model.Quotation

interface QuotationRepository {
    suspend fun getQuotations(page: Int = 1): Result<PaginatedResult<Quotation>>
    suspend fun getQuotation(id: Int): Result<Quotation>
}
