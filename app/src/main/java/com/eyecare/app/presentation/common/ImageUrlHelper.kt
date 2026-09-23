package com.eyecare.app.presentation.common

import com.eyecare.app.BuildConfig

/**
 * Builds the full URL for a catalog image.
 *
 * The API returns either an absolute URL, a legacy `/storage/...` path, or a
 * catalog-relative path such as `products/...`. Catalog-relative paths are
 * served by the public catalog bucket, which has its own base URL.
 */
fun buildImageUrl(
    path: String,
    catalogBase: String = BuildConfig.CATALOG_IMAGE_BASE_URL,
    apiBase: String = BuildConfig.API_BASE_URL,
): String {
    val normalizedPath = path.trim()
    if (normalizedPath.isEmpty()) return normalizedPath

    if (normalizedPath.startsWith("http://", ignoreCase = true) ||
        normalizedPath.startsWith("https://", ignoreCase = true)
    ) {
        return normalizedPath
    }

    val storageBase = apiBase.substringBefore("/api").trimEnd('/')
    val pathWithoutLeadingSlash = normalizedPath.trimStart('/')
    if (pathWithoutLeadingSlash.startsWith("storage/", ignoreCase = true)) {
        return "$storageBase/$pathWithoutLeadingSlash"
    }

    val normalizedCatalogBase = catalogBase.trim().trimEnd('/')
    return if (normalizedCatalogBase.isNotEmpty()) {
        "$normalizedCatalogBase/$pathWithoutLeadingSlash"
    } else {
        // Keep local/dev builds working until the catalog bucket URL is supplied.
        "$storageBase/storage/$pathWithoutLeadingSlash"
    }
}

/** Builds a full API URL from a relative API path, including routes beginning with `/api/`. */
fun buildApiUrl(path: String, apiBase: String = BuildConfig.API_BASE_URL): String {
    val normalizedPath = path.trim()
    if (normalizedPath.isEmpty()) return normalizedPath
    if (normalizedPath.startsWith("http://", ignoreCase = true) ||
        normalizedPath.startsWith("https://", ignoreCase = true)
    ) {
        return normalizedPath
    }

    val normalizedBase = apiBase.trimEnd('/')
    val origin = normalizedBase.substringBefore("/api/").trimEnd('/')
    val relativePath = normalizedPath.trimStart('/')
    return if (relativePath.startsWith("api/", ignoreCase = true)) {
        "$origin/$relativePath"
    } else {
        "$normalizedBase/$relativePath"
    }
}
