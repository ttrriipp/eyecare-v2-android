package com.eyecare.app.presentation.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.eyecare.app.domain.repository.AttachmentDownload
import java.io.File

/**
 * Writes a downloaded attachment to the app's cache dir and hands it to whatever app the
 * device has registered for its MIME type via the standard "Open with" chooser - the app has
 * no in-house viewer for non-image files (PDFs, docs), so this is the only way to view them.
 */
fun openDownloadedAttachment(context: Context, download: AttachmentDownload) {
    val attachmentsDir = File(context.cacheDir, "attachments").apply { mkdirs() }
    val file = File(attachmentsDir, download.fileName)
    file.writeBytes(download.bytes)

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, download.mimeType.ifBlank { "*/*" })
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    try {
        context.startActivity(Intent.createChooser(intent, "Open with"))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No app found that can open this file", Toast.LENGTH_SHORT).show()
    }
}
