package com.onechat.validation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.adaptivekt.components.AdaptiveBadge
import io.github.adaptivekt.components.AdaptiveBadgeTone
import io.github.adaptivekt.components.AdaptiveButton
import io.github.adaptivekt.components.AdaptiveTextField
import io.github.adaptivekt.core.AdaptiveBreakpoint
import io.github.adaptivekt.core.AdaptiveColorSchemes
import io.github.adaptivekt.core.AdaptiveTheme
import io.github.adaptivekt.core.breakpointForWidth
import io.github.adaptivekt.feedback.AdaptiveEmptyState
import io.github.adaptivekt.feedback.AdaptiveLoadingState
import io.github.adaptivekt.layout.AdaptiveContainer

data class ConversationProjection(val id: String, val label: String)
data class MessageProjection(val id: String, val conversationId: String, val text: String)

@Composable
fun OneChatMavenConsumer(
    conversations: List<ConversationProjection>,
    messages: List<MessageProjection>,
    onSend: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    dark: Boolean = false,
) {
    var selected by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf("") }
    AdaptiveTheme(colorScheme = if (dark) AdaptiveColorSchemes.defaultDark() else AdaptiveColorSchemes.defaultLight()) {
        BoxWithConstraints(modifier.fillMaxSize().background(AdaptiveTheme.colors.background)) {
            val breakpoint = breakpointForWidth(maxWidth)
            val wide = breakpoint == AdaptiveBreakpoint.Expanded || breakpoint == AdaptiveBreakpoint.Large
            val list: @Composable () -> Unit = {
                Column {
                    AdaptiveTextField(value = query, onValueChange = { query = it }, placeholder = "Buscar")
                    val matching = conversations.filter { it.label.contains(query, ignoreCase = true) }
                    if (matching.isEmpty()) {
                        AdaptiveEmptyState(title = "No hay conversaciones")
                    }
                    matching.forEach { item ->
                        Row(Modifier.fillMaxWidth().clickable { selected = item.id }.padding(12.dp)) {
                            BasicText(item.label, style = androidx.compose.ui.text.TextStyle(color = AdaptiveTheme.colors.textPrimary))
                        }
                    }
                }
            }
            val detail: @Composable () -> Unit = {
                Column {
                    if (selected == null) {
                        AdaptiveEmptyState(title = "Selecciona un chat")
                    } else {
                        AdaptiveBadge(text = "En línea", tone = AdaptiveBadgeTone.Success)
                        messages.filter { it.conversationId == selected }.forEach {
                            BasicText(it.text, style = androidx.compose.ui.text.TextStyle(color = AdaptiveTheme.colors.textPrimary))
                        }
                        AdaptiveTextField(value = draft, onValueChange = { draft = it }, placeholder = "Mensaje")
                        AdaptiveButton(text = "Enviar", enabled = draft.isNotBlank(), onClick = {
                            val id = selected
                            if (id != null && draft.isNotBlank()) {
                                onSend(id, draft)
                                draft = ""
                            }
                        })
                    }
                }
            }
            if (wide) {
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxWidth(0.3f)) { list() }
                    Box(Modifier.weight(1f).fillMaxHeight()) { detail() }
                }
            } else {
                AdaptiveContainer {
                    if (selected == null) list() else Column {
                        AdaptiveButton("Volver", onClick = { selected = null })
                        detail()
                    }
                }
            }
        }
    }
}

@Composable
fun OneChatLoadingProbe() {
    AdaptiveTheme { AdaptiveLoadingState(message = "Conectando...") }
}
