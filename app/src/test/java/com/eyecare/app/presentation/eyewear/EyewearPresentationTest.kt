package com.eyecare.app.presentation.eyewear

import com.eyecare.app.domain.model.OpticalOrderReference
import com.eyecare.app.domain.model.Quotation
import com.eyecare.app.domain.model.QuotationItem
import com.eyecare.app.domain.model.QuotationItemType
import com.eyecare.app.domain.model.QuotationStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class EyewearPresentationTest {

    private fun createQuotation(
        status: QuotationStatus = QuotationStatus.PRESENTED,
        presentedAt: String? = "2026-08-01T10:05:00Z",
        createdAt: String = "2026-08-01T10:00:00Z",
        validUntil: String? = "2026-09-01T00:00:00Z",
        opticalOrder: OpticalOrderReference? = null,
        items: List<QuotationItem> = listOf(
            QuotationItem(1, QuotationItemType.PRODUCT, "Progressive lens", 1, BigDecimal("1200.00"), BigDecimal("1200.00"), null, null, null),
        ),
    ) = Quotation(
        id = 1,
        quotationNumber = "Q-2026-001",
        status = status,
        validUntil = validUntil,
        subtotal = BigDecimal("1500.00"),
        discountAmount = BigDecimal("100.00"),
        total = BigDecimal("1400.00"),
        notes = null,
        createdAt = createdAt,
        presentedAt = presentedAt,
        confirmedAt = null,
        opticalOrder = opticalOrder,
        items = items,
    )

    @Test
    fun `estimateStatusLabel maps all statuses`() {
        assertEquals("Awaiting confirmation", estimateStatusLabel(QuotationStatus.PRESENTED))
        assertEquals("Confirmed", estimateStatusLabel(QuotationStatus.ACCEPTED))
        assertEquals("Declined", estimateStatusLabel(QuotationStatus.DECLINED))
        assertEquals("Expired", estimateStatusLabel(QuotationStatus.EXPIRED))
        assertEquals("Status unavailable", estimateStatusLabel(QuotationStatus.UNKNOWN))
    }

    @Test
    fun `estimateCardTitle uses first item description`() {
        val q = createQuotation()
        assertEquals("Progressive lens", estimateCardTitle(q))
    }

    @Test
    fun `estimateCardTitle appends count for multiple items`() {
        val items = listOf(
            QuotationItem(1, QuotationItemType.PRODUCT, "Lens", 1, BigDecimal("100.00"), BigDecimal("100.00"), null, null, null),
            QuotationItem(2, QuotationItemType.SERVICE, "Fitting", 1, BigDecimal("50.00"), BigDecimal("50.00"), null, null, null),
            QuotationItem(3, QuotationItemType.PRODUCT, "Coating", 1, BigDecimal("30.00"), BigDecimal("30.00"), null, null, null),
        )
        val q = createQuotation(items = items)
        assertEquals("Lens and 2 more", estimateCardTitle(q))
    }

    @Test
    fun `estimateCardTitle returns Estimate for empty items`() {
        val q = createQuotation(items = emptyList())
        assertEquals("Estimate", estimateCardTitle(q))
    }

    @Test
    fun `estimateDateLabel prefers presentedAt`() {
        val q = createQuotation(presentedAt = "2026-08-01T10:05:00Z", createdAt = "2026-08-01T10:00:00Z")
        val (label, _) = estimateDateLabel(q)
        assertEquals("Presented", label)
    }

    @Test
    fun `estimateDateLabel falls back to createdAt when presentedAt is null`() {
        val q = createQuotation(presentedAt = null)
        val (label, _) = estimateDateLabel(q)
        assertEquals("Created", label)
    }

    @Test
    fun `estimateDateLabel falls back to createdAt when presentedAt is blank`() {
        val q = createQuotation(presentedAt = "")
        val (label, _) = estimateDateLabel(q)
        assertEquals("Created", label)
    }

    @Test
    fun `formatPeso formats correctly`() {
        assertEquals("\u20B11,400.00", formatPeso(BigDecimal("1400.00")))
        assertEquals("\u20B10.00", formatPeso(BigDecimal("0.00")))
    }
}
