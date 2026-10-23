package com.onechat.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier
import io.github.adaptivekt.core.AdaptiveTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Let Android draw edge-to-edge, then inset interactive content away from
        // cutouts/status bar, gesture navigation and the on-screen keyboard.
        enableEdgeToEdge()
        setContent {
            AdaptiveTheme {
                Box(
                    Modifier.fillMaxSize()
                        .background(AdaptiveTheme.colors.background)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .imePadding()
                ) {
                    OneChatApp()
                }
            }
        }
    }
}
