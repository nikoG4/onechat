package com.onechat.client

import android.os.Bundle
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import android.content.res.Configuration
import android.graphics.Color
import androidx.activity.SystemBarStyle
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
    private var pendingConversation by mutableStateOf<String?>(null)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingConversation = intent.getStringExtra("conversation_id")
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingConversation = intent.getStringExtra("conversation_id")
        // Let Android draw edge-to-edge, then inset interactive content away from
        // cutouts/status bar, gesture navigation and the on-screen keyboard.
        val dark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val barStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
        else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
        // The decor must have a matching fallback behind transparent system bars.
        window.decorView.setBackgroundColor(if (dark) Color.rgb(11, 17, 32) else Color.rgb(248, 250, 252))
        setContent {
            AdaptiveTheme {
                Box(
                    Modifier.fillMaxSize()
                        .background(AdaptiveTheme.colors.background)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .imePadding()
                ) {
                    OneChatApp(
                        openConversationId = pendingConversation,
                        onConversationOpened = { pendingConversation = null },
                    )
                }
            }
        }
    }
}
