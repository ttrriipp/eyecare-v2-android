package com.eyecare.app.domain.repository

import com.eyecare.app.domain.model.SavedFrame
import com.eyecare.app.domain.model.SavedFramePage

interface SavedFrameRepository {
    suspend fun getSavedFrames(page: Int = 1, perPage: Int = 15): Result<SavedFramePage>
    suspend fun save(productVariantId: Int): Result<SavedFrame>
    suspend fun remove(productVariantId: Int): Result<Unit>
}
