package com.onechat.client

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberIncomingNotifier(): IncomingNotifier = remember {
    IncomingNotifier { _, _, _ -> Unit }
}

@Composable
actual fun NotificationPermissionPrompt() = Unit
