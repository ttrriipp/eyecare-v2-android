package com.eyecare.app.domain.repository

import com.eyecare.app.domain.model.OpticalOrder
import com.eyecare.app.domain.model.PaymentProofResult
import com.eyecare.app.domain.model.PaymentProofUpload
import com.eyecare.app.domain.model.RatingResult

interface OpticalOrderRepository {
    suspend fun getOpticalOrders(filter: String? = null, page: Int = 1): Result<PaginatedResult<OpticalOrder>>
    suspend fun getOpticalOrder(id: Int): Result<OpticalOrder>
    suspend fun rateItem(itemId: Int, rating: Int, comment: String? = null): Result<RatingResult>
    suspend fun uploadPaymentProof(orderId: Int, proof: PaymentProofUpload): Result<PaymentProofResult>
}
