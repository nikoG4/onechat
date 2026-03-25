package org.nko.chat.util

import androidx.compose.ui.graphics.ImageBitmap
import java.util.Base64

expect fun ImageBitmap.toByteArray(): ByteArray

expect fun ByteArray.toImageBitmap(): ImageBitmap

fun imageBitmapToBase64(imageBitmap: ImageBitmap): String {
    val byteArray = imageBitmap.toByteArray()
    return byteArray.toBase64()
}

fun base64ToImageBitmap(base64: String): ImageBitmap {
    val byteArray = base64.fromBase64()

    return byteArray.toImageBitmap()
}

fun ByteArray.toBase64(): String {
    return Base64.getEncoder().encodeToString(this)
}

fun String.fromBase64(): ByteArray {
    return Base64.getDecoder().decode(this)
}