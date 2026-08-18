package com.eyecare.app.presentation.common

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.eyecare.app.domain.repository.AttachmentDownload
import java.io.File

private const val GALLERY_SUBDIRECTORY = "Eyecare"

/**
 * Saves a downloaded image attachment into the device's Photos/Gallery, distinct from
 * openDownloadedAttachment's "open with another app" flow. Uses scoped-storage MediaStore
 * inserts on API 29+ (no permission needed); on API 26-28 (this app's minSdk, pre-scoped-storage)
 * writes to the app's own external-files Pictures dir instead - also permission-free - and
 * triggers a media scan so the file still shows up in gallery apps.
 */
fun saveImageToGallery(context: Context, download: AttachmentDownload) {
    val mimeType = download.mimeType.ifBlank { "image/jpeg" }
    val saved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        saveViaMediaStore(context, download, mimeType)
    } else {
        saveViaLegacyExternalFiles(context, download, mimeType)
    }
    Toast.makeText(
        context,
        if (saved) "Saved to Photos" else "Failed to save image",
        Toast.LENGTH_SHORT,
    ).show()
}

private fun saveViaMediaStore(context: Context, download: AttachmentDownload, mimeType: String): Boolean = try {
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, download.fileName)
        put(MediaStore.Images.Media.MIME_TYPE, mimeType)
        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$GALLERY_SUBDIRECTORY")
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    if (uri == null) {
        false
    } else {
        resolver.openOutputStream(uri)?.use { it.write(download.bytes) }
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        true
    }
} catch (_: Exception) {
    false
}

private fun saveViaLegacyExternalFiles(context: Context, download: AttachmentDownload, mimeType: String): Boolean = try {
    val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), GALLERY_SUBDIRECTORY)
        .apply { mkdirs() }
    val file = File(dir, download.fileName)
    file.writeBytes(download.bytes)
    MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf(mimeType), null)
    true
} catch (_: Exception) {
    false
}
