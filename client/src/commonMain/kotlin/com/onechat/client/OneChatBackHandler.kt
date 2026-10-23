package com.onechat.client

import androidx.compose.runtime.Composable

/**
 * Handles a chat's own navigation only while a nested screen is shown.
 * Android implements system Back; other targets keep their native behavior.
 */
@Composable
expect fun OneChatBackHandler(enabled: Boolean, onBack: () -> Unit)
