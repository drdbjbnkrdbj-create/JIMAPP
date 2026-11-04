package com.example.ui.library

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.ui.JimViewModel

@Composable
fun rememberDocumentImportLauncher(
    viewModel: JimViewModel,
    courseId: Long?
): () -> Unit {
    val context = LocalContext.current
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) importDocument(context, uri, viewModel, courseId)
    }

    return remember(filePicker) {
        { filePicker.launch(arrayOf("*/*")) }
    }
}

private fun importDocument(
    context: Context,
    uri: Uri,
    viewModel: JimViewModel,
    courseId: Long?
) {
    try {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    } catch (error: SecurityException) {
        Toast.makeText(context, "تعذر الاحتفاظ بإذن قراءة الملف.", Toast.LENGTH_LONG).show()
        return
    }

    var fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "مستند"
    var fileSizeBytes: Long? = null
    try {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameColumn >= 0) fileName = cursor.getString(nameColumn) ?: fileName
                if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) {
                    fileSizeBytes = cursor.getLong(sizeColumn)
                }
            }
        }
    } catch (error: Exception) {
        Toast.makeText(context, "تعذر قراءة معلومات الملف.", Toast.LENGTH_LONG).show()
        return
    }

    val mimeType = context.contentResolver.getType(uri)
        ?: MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(fileName.substringAfterLast('.', "").lowercase())
        ?: "*/*"
    val extension = fileName.substringAfterLast('.', "").lowercase()
    val fileType = when {
        mimeType == "application/pdf" || extension == "pdf" -> "PDF"
        extension in setOf("doc", "docx", "odt") || mimeType.contains("word", ignoreCase = true) -> "WORD"
        extension in setOf("ppt", "pptx", "odp") ||
            mimeType.contains("presentation", ignoreCase = true) ||
            mimeType.contains("powerpoint", ignoreCase = true) -> "PPTX"
        mimeType.startsWith("image/") -> "IMAGE"
        else -> "FILE"
    }
    val title = fileName.substringBeforeLast('.', fileName).ifBlank { fileName }
    viewModel.addDocument(
        courseId = courseId,
        title = title,
        fileType = fileType,
        fileSize = formatFileSize(fileSizeBytes),
        category = "محاضرات",
        contentSnippet = "",
        uri = uri.toString()
    )
}

private fun formatFileSize(bytes: Long?): String {
    if (bytes == null || bytes < 0) return "غير معروف"
    if (bytes < 1024) return "$bytes بايت"
    val kilobytes = bytes / 1024.0
    if (kilobytes < 1024) return String.format("%.1f ك.ب", kilobytes)
    val megabytes = kilobytes / 1024.0
    if (megabytes < 1024) return String.format("%.1f م.ب", megabytes)
    return String.format("%.1f ج.ب", megabytes / 1024.0)
}
