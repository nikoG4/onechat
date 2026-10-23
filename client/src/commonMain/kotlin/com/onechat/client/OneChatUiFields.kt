package com.onechat.client

import androidx.compose.material.TextFieldColors
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.github.adaptivekt.core.AdaptiveTheme

/** Every Material input must use AdaptiveKT semantic foreground colors. */
@Composable
fun oneChatFieldColors(): TextFieldColors = TextFieldDefaults.outlinedTextFieldColors(
    textColor = AdaptiveTheme.colors.textPrimary,
    cursorColor = AdaptiveTheme.colors.primary,
    focusedBorderColor = AdaptiveTheme.colors.primary,
    unfocusedBorderColor = AdaptiveTheme.colors.borderStrong,
    disabledBorderColor = AdaptiveTheme.colors.border,
    focusedLabelColor = AdaptiveTheme.colors.primary,
    unfocusedLabelColor = AdaptiveTheme.colors.textMuted,
    placeholderColor = AdaptiveTheme.colors.textMuted,
    leadingIconColor = AdaptiveTheme.colors.textSecondary,
    trailingIconColor = AdaptiveTheme.colors.textSecondary,
    backgroundColor = Color.Transparent,
)
