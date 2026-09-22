package com.eyecare.app.presentation.eyewear

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

data class SelectedPaymentProof(
    val file: File,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val displayName: String,
)

data class PaymentProofSelectionResult(
    val proof: SelectedPaymentProof? = null,
    val errorMessage: String? = null,
)

fun preparePaymentProof(context: Context, uri: Uri): PaymentProofSelectionResult {
    val resolver = context.contentResolver
    val displayName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
        ?: "payment-proof"
    val mimeType = resolver.getType(uri) ?: mimeTypeFromName(displayName)
    PaymentProofInspector.validateMimeType(mimeType)?.let { return PaymentProofSelectionResult(errorMessage = it) }

    val extension = when (mimeType.lowercase()) {
        "image/png" -> ".png"
        else -> ".jpg"
    }
    val temporaryFile = File.createTempFile("payment-proof-", extension, context.cacheDir)
    try {
        resolver.openInputStream(uri)?.use { input ->
            temporaryFile.outputStream().use { output -> input.copyTo(output) }
        } ?: error("The selected image could not be opened.")
    } catch (_: Exception) {
        temporaryFile.delete()
        return PaymentProofSelectionResult(errorMessage = "The selected image could not be read.")
    }

    val sizeError = PaymentProofInspector.validateFileSize(temporaryFile.length())
    if (sizeError != null) {
        temporaryFile.delete()
        return PaymentProofSelectionResult(errorMessage = sizeError)
    }

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(temporaryFile.absolutePath, bounds)
    val dimensionsError = PaymentProofInspector.validateDimensions(bounds.outWidth, bounds.outHeight)
    if (dimensionsError != null) {
        temporaryFile.delete()
        return PaymentProofSelectionResult(errorMessage = dimensionsError)
    }

    return PaymentProofSelectionResult(
        proof = SelectedPaymentProof(
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
