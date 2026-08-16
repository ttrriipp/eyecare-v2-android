package com.eyecare.app.domain.repository

import com.eyecare.app.domain.model.ArAsset
import com.eyecare.app.domain.model.ArAssetLoadResult

/** Loads one trusted catalog asset without exposing transport or storage details to callers. */
interface ArAssetRepository {
    suspend fun load(variantId: Int, asset: ArAsset): ArAssetLoadResult
}
