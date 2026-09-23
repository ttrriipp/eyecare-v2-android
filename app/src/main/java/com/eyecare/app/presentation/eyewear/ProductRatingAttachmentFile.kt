package com.eyecare.app.presentation.eyewear

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import com.eyecare.app.domain.model.ProductRatingAttachment
import java.io.File

data class ProductRatingAttachmentSelection(
    val attachment: ProductRatingAttachment? = null,
    val errorMessage: String? = null,
)

fun prepareProductRatingAttachment(context: Context, uri: Uri): ProductRatingAttachmentSelection {
    val resolver = context.contentResolver
    val displayName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        ?: "review-photo"
    val mimeType = resolver.getType(uri) ?: ratingImageMimeTypeFromName(displayName)
    PaymentProofInspector.validateMimeType(mimeType)?.let {
        return ProductRatingAttachmentSelection(errorMessage = it)
    }

    val extension = if (mimeType.equals("image/png", ignoreCase = true)) ".png" else ".jpg"
    val temporaryFile = File.createTempFile("rating-photo-", extension, context.cacheDir)
    try {
        resolver.openInputStream(uri)?.use { input ->
            temporaryFile.outputStream().use { output -> input.copyTo(output) }
        } ?: error("The selected image could not be opened.")
    } catch (_: Exception) {
        temporaryFile.delete()
        return ProductRatingAttachmentSelection(errorMessage = "The selected image could not be read.")
    }

    PaymentProofInspector.validateFileSize(temporaryFile.length())?.let {
        temporaryFile.delete()
        return ProductRatingAttachmentSelection(errorMessage = it)
    }

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(temporaryFile.absolutePath, bounds)
    PaymentProofInspector.validateDimensions(bounds.outWidth, bounds.outHeight)?.let {
        temporaryFile.delete()
        return ProductRatingAttachmentSelection(errorMessage = it)
    }

    return ProductRatingAttachmentSelection(
        attachment = ProductRatingAttachment(
            imageFile = temporaryFile,
            mimeType = mimeType,
            width = bounds.outWidth,
            height = bounds.outHeight,
        ),
    )
}

private fun ratingImageMimeTypeFromName(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
    "png" -> "image/png"
    "jpg", "jpeg" -> "image/jpeg"
    else -> ""
}
