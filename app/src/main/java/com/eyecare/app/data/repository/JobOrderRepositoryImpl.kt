package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.JobOrderApiService
import com.eyecare.app.data.remote.dto.ApiErrorBody
import com.eyecare.app.data.remote.dto.FrameRatingDtos
import com.eyecare.app.data.remote.dto.JobOrderDtos
import com.eyecare.app.domain.model.FrameRating
import com.eyecare.app.domain.model.FrameRatingRevision
import com.eyecare.app.domain.model.JobOrder
import com.eyecare.app.domain.model.JobOrderItem
import com.eyecare.app.domain.model.JobOrderStatus
import com.eyecare.app.domain.repository.JobOrderRepository
import com.eyecare.app.domain.repository.PaginatedResult
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import javax.inject.Inject

class JobOrderRepositoryImpl @Inject constructor(
    private val api: JobOrderApiService,
    private val json: Json,
) : JobOrderRepository {

    override suspend fun getJobOrders(page: Int): Result<PaginatedResult<JobOrder>> = runCatching {
        val response = api.getJobOrders(page = page)
        PaginatedResult(
            data = response.data.map { it.toDomain() },
            currentPage = response.meta?.currentPage ?: 1,
            lastPage = response.meta?.lastPage ?: 1,
            total = response.meta?.total ?: response.data.size,
        )
    }

    override suspend fun getJobOrder(id: Int): Result<JobOrder> = runCatching {
        api.getJobOrder(id).data.toDomain()
    }

    override suspend fun submitRating(
        jobOrderItemId: Int,
        productVariantId: Int,
        rating: Int,
        comment: String?,
        dispensingEventId: Int?,
    ): Result<FrameRating> = runCatching {
        api.submitRating(
            jobOrderItemId,
            FrameRatingDtos.SubmitRatingRequest(
                productVariantId = productVariantId,
                rating = rating,
                comment = comment,
                dispensingEventId = dispensingEventId,
            ),
        ).data.toDomain()
    }.recoverCatching { throwable ->
        if (throwable is HttpException) {
            val body = throwable.response()?.errorBody()?.use { it.string() } ?: ""
            val parsed = json.decodeFromString<ApiErrorBody>(body)
            throw FrameRatingError(throwable.code(), parsed.message, parsed.errors)
        }
        throw throwable
    }

    private fun JobOrderDtos.JobOrderDto.toDomain() = JobOrder(
        id = id,
        jobOrderNumber = jobOrderNumber,
        patientId = patientId,
        encounterId = encounterId,
        prescriptionId = prescriptionId,
        quotationRevisionId = quotationRevisionId,
        status = JobOrderStatus.from(status),
        totalAmount = totalAmount,
        notes = notes,
        startedAt = startedAt,
        readyAt = readyAt,
        dispensedAt = dispensedAt,
        cancelledAt = cancelledAt,
        items = items.map { it.toDomain() },
    )

    private fun JobOrderDtos.JobOrderItemDto.toDomain() = JobOrderItem(
        id = id,
        description = description,
        quantity = quantity,
        unitPrice = unitPrice,
        amount = amount,
        productVariantId = productVariantId,
    )

    private fun FrameRatingDtos.FrameRatingDto.toDomain() = FrameRating(
        id = id,
        patientId = patientId,
        productVariantId = productVariantId,
        dispensingEventId = dispensingEventId,
        rating = rating,
        comment = comment,
        currentRevisionId = currentRevisionId,
        isHidden = isHidden,
        moderationReason = moderationReason,
        revisions = revisions.map { it.toDomain() },
    )

    private fun FrameRatingDtos.FrameRatingRevisionDto.toDomain() = FrameRatingRevision(
        id = id,
        frameRatingId = frameRatingId,
        revisionNumber = revisionNumber,
        rating = rating,
        comment = comment,
        revisedBy = revisedBy,
        revisedAt = revisedAt,
    )
}

class FrameRatingError(
    val httpCode: Int,
    override val message: String,
    val fieldErrors: Map<String, List<String>>? = null,
) : Exception(message)
