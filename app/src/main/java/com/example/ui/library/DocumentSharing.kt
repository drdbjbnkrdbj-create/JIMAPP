package com.example.ui.library

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.model.LibraryDocumentEntity

fun shareText(context: Context, chooserTitle: String, text: String) {
    if (text.isBlank()) return

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }

    try {
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    } catch (error: ActivityNotFoundException) {
        Toast.makeText(context, "لا يوجد تطبيق لمشاركة المحتوى.", Toast.LENGTH_LONG).show()
    }
}

fun shareDocuments(context: Context, documents: List<LibraryDocumentEntity>) {
    if (documents.isEmpty()) return

    val uris = documents.mapNotNull { it.uri?.let(Uri::parse) }
    val text = documents
        .filter { it.uri == null }
        .joinToString("\n\n") { "${it.title}\n${it.contentSnippet.take(500)}" }
        .ifBlank { null }

    val intent = when {
        uris.size > 1 -> Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }
        uris.size == 1 -> Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, uris.single())
        }
        else -> Intent(Intent.ACTION_SEND)
    }.apply {
        if (text != null) putExtra(Intent.EXTRA_TEXT, text)
        if (uris.isNotEmpty()) {
            val mimeTypes = uris.map { context.contentResolver.getType(it) ?: "*/*" }.distinct()
            type = mimeTypes.singleOrNull() ?: "*/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newUri(context.contentResolver, "ملفات دراسية", uris.first()).apply {
                uris.drop(1).forEach { addItem(ClipData.Item(it)) }
            }
        } else {
            type = "text/plain"
        }
    }

    try {
        context.startActivity(Intent.createChooser(intent, "مشاركة الملفات المحددة"))
    } catch (error: ActivityNotFoundException) {
        Toast.makeText(context, "لا يوجد تطبيق لمشاركة الملفات.", Toast.LENGTH_LONG).show()
    }
}
