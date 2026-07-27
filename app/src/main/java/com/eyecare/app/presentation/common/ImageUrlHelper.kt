package com.eyecare.app.presentation.common

import com.eyecare.app.BuildConfig

/**
 * Builds the full URL for a storage asset.
 * The API base URL includes `/api/` but storage files are at `/storage/`,
 * e.g. API = "http://host/api/" → storage = "http://host/storage/path"
 */
fun buildImageUrl(path: String, apiBase: String = BuildConfig.API_BASE_URL): String {
    if (path.startsWith("http")) return path
    val storageBase = apiBase.substringBefore("/api")
    return "$storageBase/storage/$path"
}
