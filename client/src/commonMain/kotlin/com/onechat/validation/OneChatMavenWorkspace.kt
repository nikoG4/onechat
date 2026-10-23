package com.onechat.validation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.adaptivekt.components.AdaptiveBadge
import io.github.adaptivekt.components.AdaptiveBadgeTone
import io.github.adaptivekt.components.AdaptiveButton
import io.github.adaptivekt.components.AdaptiveButtonVariant
import io.github.adaptivekt.components.AdaptiveTextField
import io.github.adaptivekt.core.AdaptiveBreakpoint
import io.github.adaptivekt.core.AdaptiveColorSchemes
import io.github.adaptivekt.core.AdaptiveTheme
import io.github.adaptivekt.core.breakpointForWidth
import io.github.adaptivekt.feedback.AdaptiveEmptyState
import io.github.adaptivekt.feedback.AdaptiveLoadingState

/** Reference presentation layer. Source dependency ONLY on Maven Central alpha01. */
data class OneChatConversation(val id: String, val title: String, val preview: String, val unread: Int = 0)
data class OneChatBubble(val id: String, val conversationId: String, val body: String, val mine: Boolean, val label: String)
enum class ConnectionLabel { ONLINE, CONNECTING, RECONNECTING, OFFLINE }
data class OneChatView(
    val conversations: List<OneChatConversation> = emptyList(),
    val selectedId: String? = null,
    val messages: List<OneChatBubble> = emptyList(),
    val query: String = "",
    val draft: String = "",
    val loading: Boolean = false,
    val sending: Boolean = false,
    val connection: ConnectionLabel = ConnectionLabel.OFFLINE,
    val error: String? = null,
)

/** Caller supplies the actual ViewModel state and side effects, not another mock repository. */
@Composable
fun OneChatMavenWorkspace(
    state: OneChatView,
    onSelect: (String) -> Unit,
    onBack: () -> Unit,
    onQuery: (String) -> Unit,
    onDraft: (String) -> Unit,
    onSend: (String, String) -> Unit,
    onCreateConversation: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = false,
) {
    AdaptiveTheme(colorScheme = if (darkTheme) AdaptiveColorSchemes.defaultDark() else AdaptiveColorSchemes.defaultLight()) {
        BoxWithConstraints(modifier.fillMaxSize().background(AdaptiveTheme.colors.background)) {
            val breakpoint = breakpointForWidth(maxWidth)
            val wide = breakpoint == AdaptiveBreakpoint.Expanded || breakpoint == AdaptiveBreakpoint.Large
            if (wide) {
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(3f).fillMaxHeight()) {
                        ConversationList(state, onSelect, onQuery, onCreateConversation, onRetry)
                    }
                    Box(Modifier.width(1.dp).fillMaxHeight().background(AdaptiveTheme.colors.border))
                    Box(Modifier.weight(7f).fillMaxHeight()) {
                        ChatDetail(state, onDraft, onSend)
                    }
                }
            } else if (state.selectedId == null) {
                ConversationList(state, onSelect, onQuery, onCreateConversation, onRetry)
            } else {
                Column(Modifier.fillMaxSize()) {
                    AdaptiveButton(text = "Volver", onClick = onBack, variant = AdaptiveButtonVariant.Ghost,
                        modifier = Modifier.padding(8.dp))
                    Box(Modifier.weight(1f)) { ChatDetail(state, onDraft, onSend) }
                }
            }
        }
    }
}

@Composable
private fun ConversationList(
    state: OneChatView, onSelect: (String) -> Unit, onQuery: (String) -> Unit,
    onCreate: () -> Unit, onRetry: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(AdaptiveTheme.colors.surface)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            BasicText("OneChat", style = TextStyle(color = AdaptiveTheme.colors.textPrimary, fontWeight = FontWeight.Bold))
            AdaptiveButton("Nuevo", onClick = onCreate)
        }
        AdaptiveTextField(state.query, onValueChange = onQuery, placeholder = "Buscar chats",
            modifier = Modifier.padding(horizontal = 12.dp))
        if (state.error != null) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                BasicText(state.error, modifier = Modifier.weight(1f),
                    style = TextStyle(color = AdaptiveTheme.colors.danger))
                AdaptiveButton("Reintentar", onClick = onRetry)
            }
        }
        val query = state.query.trim()
        val visible = state.conversations.filter {
            query.isEmpty() || it.title.contains(query, ignoreCase = true) ||
                it.preview.contains(query, ignoreCase = true)
        }
        when {
            state.loading -> AdaptiveLoadingState(message = "Cargando conversaciones")
            visible.isEmpty() -> AdaptiveEmptyState(
                title = if (query.isEmpty()) "No hay conversaciones" else "Sin resultados",
                description = "Inicia una conversación o cambia la búsqueda.",
            )
            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(visible, key = { it.id }) { conversation ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .background(if (conversation.id == state.selectedId)
                                AdaptiveTheme.colors.primarySubtle else AdaptiveTheme.colors.surface)
                            .clickable { onSelect(conversation.id) }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            BasicText(conversation.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                style = TextStyle(color = AdaptiveTheme.colors.textPrimary, fontWeight = FontWeight.SemiBold))
                            Spacer(Modifier.height(4.dp))
                            BasicText(conversation.preview, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                style = TextStyle(color = AdaptiveTheme.colors.textMuted))
                        }
                        if (conversation.unread > 0) AdaptiveBadge(
                            text = conversation.unread.coerceAtMost(99).toString(), tone = AdaptiveBadgeTone.Info,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatDetail(state: OneChatView, onDraft: (String) -> Unit, onSend: (String, String) -> Unit) {
    val id = state.selectedId
    val conversation = state.conversations.find { it.id == id }
    when {
        id == null -> AdaptiveEmptyState("Selecciona una conversación")
        conversation == null -> AdaptiveEmptyState("Conversación no disponible")
        else -> Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                BasicText(conversation.title, style = TextStyle(color = AdaptiveTheme.colors.textPrimary, fontWeight = FontWeight.Bold))
                val (label, tone) = when (state.connection) {
                    ConnectionLabel.ONLINE -> "En línea" to AdaptiveBadgeTone.Success
                    ConnectionLabel.CONNECTING -> "Conectando" to AdaptiveBadgeTone.Warning
                    ConnectionLabel.RECONNECTING -> "Reconectando" to AdaptiveBadgeTone.Warning
                    ConnectionLabel.OFFLINE -> "Sin conexión" to AdaptiveBadgeTone.Danger
                }
                AdaptiveBadge(label, tone = tone)
            }
            val visible = state.messages.filter { it.conversationId == id }
            if (visible.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    AdaptiveEmptyState("Sin mensajes")
                }
            } else {
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    items(visible, key = { it.id }) { msg ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = if (msg.mine) Arrangement.End else Arrangement.Start) {
                            Column(Modifier.fillMaxWidth(0.83f)
                                .background(if (msg.mine) AdaptiveTheme.colors.primarySubtle else AdaptiveTheme.colors.surface)
                                .padding(12.dp)) {
                                BasicText(msg.body, style = TextStyle(color = AdaptiveTheme.colors.textPrimary))
                                BasicText(msg.label, style = TextStyle(color = AdaptiveTheme.colors.textMuted))
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdaptiveTextField(state.draft, onValueChange = onDraft,
                    placeholder = "Mensaje", modifier = Modifier.weight(1f))
                AdaptiveButton(if (state.sending) "Enviando" else "Enviar",
                    enabled = !state.sending && state.draft.isNotBlank() && state.connection != ConnectionLabel.CONNECTING,
                    onClick = {
                        val message = state.draft.trim()
                        if (message.isNotEmpty()) onSend(id, message)
                    })
            }
        }
    }
}
