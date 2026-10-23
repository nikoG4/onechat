package com.onechat.client

import androidx.compose.runtime.Composable

/** OS alerts are a best-effort supplement to the universal in-app banner. */
fun interface IncomingNotifier {
    fun show(title: String, message: String, conversationId: String)
}

@Composable
expect fun rememberIncomingNotifier(): IncomingNotifier

@Composable
expect fun NotificationPermissionPrompt()
