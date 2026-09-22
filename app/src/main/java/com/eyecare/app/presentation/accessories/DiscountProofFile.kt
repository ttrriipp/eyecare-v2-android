package com.eyecare.app.presentation.accessories

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import com.eyecare.app.presentation.eyewear.PaymentProofInspector
import java.io.File

data class SelectedDiscountProof(
    val file: File,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val displayName: String,
)

data class DiscountProofSelectionResult(
    val proof: SelectedDiscountProof? = null,
    val errorMessage: String? = null,
)

fun prepareDiscountProof(context: Context, uri: Uri): DiscountProofSelectionResult {
    val resolver = context.contentResolver
    val displayName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
        ?: "discount-proof"
    val mimeType = resolver.getType(uri) ?: mimeTypeFromName(displayName)
    PaymentProofInspector.validateMimeType(mimeType)?.let {
        return DiscountProofSelectionResult(errorMessage = it)
    }

    val extension = if (mimeType.equals("image/png", ignoreCase = true)) ".png" else ".jpg"
    val temporaryFile = File.createTempFile("discount-proof-", extension, context.cacheDir)
    try {
        resolver.openInputStream(uri)?.use { input ->
            temporaryFile.outputStream().use { output -> input.copyTo(output) }
        } ?: error("The selected image could not be opened.")
    } catch (_: Exception) {
        temporaryFile.delete()
        return DiscountProofSelectionResult(errorMessage = "The selected image could not be read.")
    }

    PaymentProofInspector.validateFileSize(temporaryFile.length())?.let {
        temporaryFile.delete()
        return DiscountProofSelectionResult(errorMessage = it)
    }

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(temporaryFile.absolutePath, bounds)
    PaymentProofInspector.validateDimensions(bounds.outWidth, bounds.outHeight)?.let {
        temporaryFile.delete()
        return DiscountProofSelectionResult(errorMessage = it)
    }

    return DiscountProofSelectionResult(
        proof = SelectedDiscountProof(
            file = temporaryFile,
            mimeType = mimeType,
            width = bounds.outWidth,
            height = bounds.outHeight,
            displayName = displayName,
        ),
    )
}

private fun mimeTypeFromName(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
    "png" -> "image/png"
    "jpg", "jpeg" -> "image/jpeg"
    else -> ""
}
