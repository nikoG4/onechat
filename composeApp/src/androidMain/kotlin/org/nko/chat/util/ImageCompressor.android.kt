package org.nko.chat.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream

actual fun compressImageBitmap(
    imageBitmap: ImageBitmap,
    format: CompressFormat,
    quality: Int
): ImageBitmap {
    // Convertir ImageBitmap a Bitmap de Android
    val androidBitmap = imageBitmap.asAndroidBitmap()

    // Elegir formato de compresión
    val androidCompressFormat = when (format) {
        CompressFormat.PNG -> Bitmap.CompressFormat.PNG
        CompressFormat.JPEG -> Bitmap.CompressFormat.JPEG
    }

    // Comprimir el Bitmap a un ByteArrayOutputStream
    val outputStream = ByteArrayOutputStream()
    androidBitmap.compress(androidCompressFormat, quality, outputStream)

    // Convertir los datos comprimidos de nuevo a un Bitmap
    val compressedByteArray = outputStream.toByteArray()
    val compressedBitmap = BitmapFactory.decodeByteArray(compressedByteArray, 0, compressedByteArray.size)

    // Convertir el Bitmap comprimido a ImageBitmap y devolverlo
    return compressedBitmap.asImageBitmap()
}