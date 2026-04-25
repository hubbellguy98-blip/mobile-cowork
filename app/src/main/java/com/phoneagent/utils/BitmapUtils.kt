package com.phoneagent.utils

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

fun Bitmap.toBase64(quality: Int = 70): String {
    val outputStream = ByteArrayOutputStream()
    this.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
    val byteArray = outputStream.toByteArray()
    val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)
    return "data:image/jpeg;base64,$base64String"
}

fun Bitmap.resize(maxWidth: Int = 1080, maxHeight: Int = 1920): Bitmap {
    val width = this.width
    val height = this.height
    
    if (width <= maxWidth && height <= maxHeight) return this
    
    val ratioBitmap = width.toFloat() / height.toFloat()
    val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()
    
    var finalWidth = maxWidth
    var finalHeight = maxHeight
    
    if (ratioMax > ratioBitmap) {
        finalWidth = (maxHeight.toFloat() * ratioBitmap).toInt()
    } else {
        finalHeight = (maxWidth.toFloat() / ratioBitmap).toInt()
    }
    
    return Bitmap.createScaledBitmap(this, finalWidth, finalHeight, true)
}

fun Bitmap.cropStatusBar(): Bitmap {
    // Assuming roughly 80px status bar for 1080p width
    val statusBarHeight = 80
    if (this.height <= statusBarHeight) return this
    return Bitmap.createBitmap(this, 0, statusBarHeight, this.width, this.height - statusBarHeight)
}

fun createScreenshotFile(context: Context, bitmap: Bitmap): File {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val dir = File(context.filesDir, "screenshots")
    if (!dir.exists()) dir.mkdirs()
    
    val file = File(dir, "screenshot_$timestamp.jpg")
    file.outputStream().use {
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, it)
    }
    return file
}
