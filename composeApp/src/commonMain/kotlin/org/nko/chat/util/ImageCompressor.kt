package org.nko.chat.util

import androidx.compose.ui.graphics.ImageBitmap

expect fun compressImageBitmap(imageBitmap: ImageBitmap, format: CompressFormat, quality: Int): ImageBitmap

enum class CompressFormat {
    PNG, JPEG
}
