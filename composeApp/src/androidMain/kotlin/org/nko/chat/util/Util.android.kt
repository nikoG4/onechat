package org.nko.chat.util

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.ByteArrayOutputStream
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap

actual fun ImageBitmap.toByteArray(): ByteArray {
    val androidBitmap: Bitmap = this.asAndroidBitmap()
    val byteArrayOutputStream = ByteArrayOutputStream()
    androidBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
    return byteArrayOutputStream.toByteArray()
}




actual fun ByteArray.toImageBitmap(): ImageBitmap {
    val bitmap = BitmapFactory.decodeByteArray(this, 0, this.size)
    return bitmap.asImageBitmap()
}