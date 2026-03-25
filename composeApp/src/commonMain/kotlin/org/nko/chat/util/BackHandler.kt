package org.nko.chat.util

import androidx.compose.runtime.Composable

@Composable
expect fun BackHandler(onBackPressed: () -> Unit)
