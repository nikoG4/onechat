package org.nko.chat

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Chat KMP") {
        ChatApp(org.nko.chat.util.DriverFactory())
    }
}