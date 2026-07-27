package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.InvoiceApiService
import com.eyecare.app.data.remote.dto.InvoiceDtos
import com.eyecare.app.domain.model.Invoice
import com.eyecare.app.domain.model.InvoiceItem
import com.eyecare.app.domain.model.InvoicePayment
import com.eyecare.app.domain.model.InvoiceStatus
import com.eyecare.app.domain.repository.InvoiceRepository
import com.eyecare.app.domain.repository.PaginatedResult
import javax.inject.Inject

class InvoiceRepositoryImpl @Inject constructor(
    private val api: InvoiceApiService,
) : InvoiceRepository {

    override suspend fun getInvoices(page: Int): Result<PaginatedResult<Invoice>> = runCatching {
        val response = api.getInvoices(page = page)
        PaginatedResult(
            data = response.data.map { it.toDomain() },
            currentPage = response.meta?.currentPage ?: 1,
            lastPage = response.meta?.lastPage ?: 1,
            total = response.meta?.total ?: response.data.size,
        )
    }

    override suspend fun getInvoice(id: Int): Result<Invoice> = runCatching {
        api.getInvoice(id).data.toDomain()
    }

    private fun InvoiceDtos.InvoiceDto.toDomain() = Invoice(
        id = id,
        invoiceNumber = invoiceNumber,
        officialNumber = officialNumber,
        patientId = patientId,
        jobOrderId = jobOrderId,
        encounterId = encounterId,
        status = InvoiceStatus.from(status),
        saleType = saleType,
        soldToName = soldToName,
        subtotal = subtotal,
        discountAmount = discountAmount,
        taxAmount = taxAmount,
        total = total,
        amountPaid = amountPaid,
        balanceDue = balanceDue,
        notes = notes,
        issuedAt = issuedAt,
        items = items.map { it.toDomain() },
        payments = payments.map { it.toDomain() },
    )

    private fun InvoiceDtos.InvoiceItemDto.toDomain() = InvoiceItem(
        id = id,
        type = type,
        description = description,
        quantity = quantity,
        unitPrice = unitPrice,
        amount = amount,
    )

    private fun InvoiceDtos.InvoicePaymentDto.toDomain() = InvoicePayment(
        id = id,
        amount = amount,
        paymentMethod = paymentMethod,
        referenceNumber = referenceNumber,
        status = status,
    )
}
