package com.eyecare.app.domain.repository

import com.eyecare.app.domain.model.Accessory
import com.eyecare.app.domain.model.AccessoryQuery

interface AccessoryRepository {
    suspend fun getAccessories(query: AccessoryQuery = AccessoryQuery()): Result<PaginatedResult<Accessory>>
    suspend fun getAccessory(id: Int): Result<Accessory>
}