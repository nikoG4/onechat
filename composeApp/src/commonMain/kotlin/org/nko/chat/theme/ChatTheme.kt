package org.nko.chat.theme


import androidx.compose.ui.graphics.Color

object ChatTheme {
    val lightBackground = Color(0xFFFFFFFF)
    val darkBackground = Color(0xFF121212)

    val lightPrimary = Color(0xFF6200EE)
    val darkPrimary = Color(0xFFBB86FC)

    fun backgroundColor(isDarkTheme: Boolean): Color {
        return if (isDarkTheme) darkBackground else lightBackground
    }

    fun primaryColor(isDarkTheme: Boolean): Color {
        return if (isDarkTheme) darkPrimary else lightPrimary
    }
}
