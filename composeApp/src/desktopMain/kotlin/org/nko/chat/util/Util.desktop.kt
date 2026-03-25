package org.nko.chat.util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asDesktopBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skiko.toBufferedImage
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.imageio.ImageIO

actual fun ImageBitmap.toByteArray(): ByteArray {
    val bufferedImage: BufferedImage = this.asSkiaBitmap().toBufferedImage()
    val byteArrayOutputStream = ByteArrayOutputStream()
    try {
        ImageIO.write(bufferedImage, "PNG", byteArrayOutputStream)  // Guarda como PNG
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return byteArrayOutputStream.toByteArray()
}

actual fun ByteArray.toImageBitmap(): ImageBitmap {
    TODO()
}