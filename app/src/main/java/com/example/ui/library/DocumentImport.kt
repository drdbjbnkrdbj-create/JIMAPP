package com.example.ui.library

import android.content.Context
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.model.LibraryDocumentEntity
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

@Composable
fun DocumentOpenOptionsDialog(
    document: LibraryDocumentEntity,
    onDismiss: () -> Unit,
    onOpenInApp: () -> Unit
) {
    val context = LocalContext.current
    val canOpenInApp = document.fileType.equals("PDF", ignoreCase = true) ||
        document.fileType.equals("IMAGE", ignoreCase = true) ||
        (document.uri == null && document.contentSnippet.isNotBlank())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("فتح الملف") },
        text = {
            Text(
                if (canOpenInApp) {
                    "اختر قارئ جيم أو افتح الملف باستخدام قارئ خارجي."
                } else {
                    "هذا النوع لا يدعمه قارئ جيم. يمكنك فتحه بتطبيق خارجي."
                }
            )
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (canOpenInApp) {
                    TextButton(
                        onClick = {
                            onDismiss()
                            onOpenInApp()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("قارئ جيم")
                    }
                }
                Button(
                    onClick = {
                        openDocumentWithExternalReader(context, document)
                        onDismiss()
                    },
                    enabled = document.uri != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("قارئ خارجي")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

fun openDocumentWithExternalReader(context: Context, document: LibraryDocumentEntity) {
    val uri = document.uri?.let(Uri::parse)
    if (uri == null) {
        Toast.makeText(context, "لا يوجد ملف مرفق لفتحه.", Toast.LENGTH_LONG).show()
        return
    }

    val extension = uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase()
    val fallbackMimeType = when (extension) {
        "doc" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "odt" -> "application/vnd.oasis.opendocument.text"
        "ppt" -> "application/vnd.ms-powerpoint"
        "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        "odp" -> "application/vnd.oasis.opendocument.presentation"
        "pdf" -> "application/pdf"
        else -> when (document.fileType.uppercase()) {
            "PDF" -> "application/pdf"
            "WORD" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "PPTX" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "IMAGE" -> "image/*"
            else -> "*/*"
        }
    }
    val mimeType = context.contentResolver.getType(uri)
        ?.takeUnless { it == "application/octet-stream" || it == "*/*" }
        ?: MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(extension.orEmpty())
        ?: fallbackMimeType
    val viewIntent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, mimeType)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

    try {
        context.startActivity(Intent.createChooser(viewIntent, "فتح ${document.title} بتطبيق آخر"))
    } catch (error: ActivityNotFoundException) {
        Log.e("DocumentOpen", "No external reader is available for ${document.title}", error)
        Toast.makeText(context, "لم يتم العثور على تطبيق يدعم هذا الملف.", Toast.LENGTH_LONG).show()
    } catch (error: SecurityException) {
        Log.e("DocumentOpen", "No permission to open ${document.title}", error)
        Toast.makeText(context, "تعذر الوصول إلى الملف. أعد استيراده ثم حاول مجددًا.", Toast.LENGTH_LONG).show()
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
