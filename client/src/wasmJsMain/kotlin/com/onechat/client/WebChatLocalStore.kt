package com.onechat.client

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberChatLocalStore(): ChatLocalStore = remember { EphemeralChatStore() }
