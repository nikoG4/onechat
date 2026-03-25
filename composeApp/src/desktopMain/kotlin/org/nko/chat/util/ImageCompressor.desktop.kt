package org.nko.chat.util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

actual fun compressImageBitmap(
    imageBitmap: ImageBitmap,
    format: CompressFormat,
    quality: Int
): ImageBitmap {
    // Convertir ImageBitmap a Skia Image
    val skiaImage = Image.makeFromBitmap(imageBitmap.asSkiaBitmap())

    // Elegir formato de compresión
    val encodedFormat = when (format) {
        CompressFormat.PNG -> EncodedImageFormat.PNG
        CompressFormat.JPEG -> EncodedImageFormat.JPEG
    }

    // Comprimir la imagen
    val compressedData = skiaImage.encodeToData(encodedFormat, quality)?.bytes
        ?: throw IllegalStateException("Failed to encode image")

    // Reconstruir el ImageBitmap desde los bytes comprimidos
    val decompressedImage = Image.makeFromEncoded(compressedData)
    return decompressedImage.toComposeImageBitmap()
}