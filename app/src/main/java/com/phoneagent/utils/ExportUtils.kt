package com.phoneagent.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.phoneagent.data.local.entities.ConversationEntity
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportUtils {

    fun exportChatAsText(messages: List<ConversationEntity>): String {
        val sb = java.lang.StringBuilder()
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        sb.append("=== PhoneAgent Session Export ===\n")
        sb.append("Date: $dateStr\n\n")

        for (msg in messages) {
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(msg.timestamp))
            when (msg.role) {
                "user" -> sb.append("[User] $time: ${msg.content}\n")
                "assistant" -> sb.append("[Agent] $time: ${msg.content}\n")
                "action" -> {
                    val icon = if (msg.actionSuccess == true) "✅" else if (msg.actionSuccess == false) "❌" else ""
                    sb.append("[Action] Step ${msg.stepNumber ?: "?"}: ${msg.content} $icon\n")
                }
                else -> sb.append("[${msg.role}] $time: ${msg.content}\n")
            }
        }
        return sb.toString()
    }

    fun saveExportToFile(context: Context, content: String): Uri? {
        val filename = "phoneagent_export_${System.currentTimeMillis()}.txt"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { stream ->
                    stream.write(content.toByteArray())
                }
            }
            return uri
        } else {
            // Fallback for older Android versions
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = java.io.File(dir, filename)
            file.writeText(content)
            return Uri.fromFile(file)
        }
    }
}
