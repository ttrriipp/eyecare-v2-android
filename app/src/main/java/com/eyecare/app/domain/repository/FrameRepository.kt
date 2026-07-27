package com.eyecare.app.domain.repository

import com.eyecare.app.domain.model.Frame

interface FrameRepository {
    suspend fun getFrames(
        page: Int = 1,
        search: String? = null,
        brandId: Int? = null,
        categoryId: Int? = null,
        sort: String? = null,
    ): Result<List<Frame>>
    suspend fun getFrame(id: Int): Result<Frame>
    suspend fun hasMorePages(page: Int): Boolean
}
