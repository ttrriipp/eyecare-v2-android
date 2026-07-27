package com.eyecare.app.domain.repository

import com.eyecare.app.domain.model.Invoice

interface InvoiceRepository {
    suspend fun getInvoices(page: Int = 1): Result<PaginatedResult<Invoice>>
    suspend fun getInvoice(id: Int): Result<Invoice>
}
