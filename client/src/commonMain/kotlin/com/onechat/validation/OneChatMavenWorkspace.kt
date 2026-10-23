package com.onechat.validation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onechat.client.oneChatFieldColors
import io.github.adaptivekt.components.AdaptiveAvatar
import io.github.adaptivekt.components.AdaptiveIconButton
import io.github.adaptivekt.components.icons.AdaptiveIcons
import io.github.adaptivekt.components.AdaptiveBadge
import io.github.adaptivekt.components.AdaptiveBadgeTone
import io.github.adaptivekt.core.AdaptiveBreakpoint
import io.github.adaptivekt.core.AdaptiveTheme
import io.github.adaptivekt.core.breakpointForWidth
import io.github.adaptivekt.feedback.AdaptiveEmptyState
import io.github.adaptivekt.feedback.AdaptiveLoadingState

data class OneChatConversation(
    val id: String, val title: String, val preview: String,
    val unread: Int = 0, val peerId: String = "",
)
data class OneChatBubble(
    val id: String, val conversationId: String, val body: String,
    val mine: Boolean, val label: String, val readAt: Long? = null,
)
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

/** Uses the AdaptiveKT theme inherited from the application, never forces a light scheme. */
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
) {
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
            ChatDetail(state, onDraft, onSend, onBack = onBack)
        }
    }
}

@Composable
private fun ConversationList(
    state: OneChatView,
    onSelect: (String) -> Unit,
    onQuery: (String) -> Unit,
    onCreate: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(AdaptiveTheme.colors.surface)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 20.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    "Mensajes",
                    style = MaterialTheme.typography.h5,
                    fontWeight = FontWeight.Bold,
                    color = AdaptiveTheme.colors.textPrimary,
                )
                Text(
                    if (state.conversations.size == 1) "1 conversación" else
                        state.conversations.size.toString() + " conversaciones",
                    style = MaterialTheme.typography.caption,
                    color = AdaptiveTheme.colors.textMuted,
                )
            }
            Button(
                onClick = onCreate,
                shape = RoundedCornerShape(14.dp),
                elevation = ButtonDefaults.elevation(defaultElevation = 0.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            ) { Text("+  Nuevo", fontWeight = FontWeight.SemiBold) }
        }
        OutlinedTextField(
            value = state.query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            placeholder = { Text("Buscar conversaciones") },
            leadingIcon = {
                AdaptiveIcons.Search(size = 20.dp, tint = AdaptiveTheme.colors.textSecondary)
            },
            colors = oneChatFieldColors(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
        )
        if (state.error != null) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AdaptiveTheme.colors.dangerSubtle).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(state.error, color = AdaptiveTheme.colors.dangerText,
                    style = MaterialTheme.typography.body2, modifier = Modifier.weight(1f))
                TextButton(onClick = onRetry) { Text("Reintentar") }
            }
        }
        val query = state.query.trim()
        val visible = state.conversations.filter {
            query.isEmpty() || it.title.contains(query, ignoreCase = true) ||
                it.preview.contains(query, ignoreCase = true)
        }
        when {
            state.loading && state.conversations.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AdaptiveLoadingState(message = "Cargando conversaciones")
                }
            }
            visible.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    AdaptiveEmptyState(
                        title = if (query.isEmpty()) "Empezá una conversación" else "Sin resultados",
                        description = if (query.isEmpty())
                            "Buscá un contacto con el botón Nuevo para escribirle."
                        else "Probá con otro nombre de usuario.",
                    )
                }
            }
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(visible, key = { it.id }) { conversation ->
                    val selected = conversation.id == state.selectedId
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) AdaptiveTheme.colors.primarySubtle else AdaptiveTheme.colors.surface)
                            .clickable { onSelect(conversation.id) }
                            .padding(horizontal = 12.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Placeholder initials are provided by published AdaptiveKT.
                        AdaptiveAvatar(name = conversation.title, size = 46.dp)
                        Column(Modifier.weight(1f)) {
                            Text(conversation.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.SemiBold,
                                color = AdaptiveTheme.colors.textPrimary,
                                style = MaterialTheme.typography.body1)
                            Spacer(Modifier.height(3.dp))
                            Text(
                                conversation.preview.ifBlank { "Escribí tu primer mensaje" },
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                color = AdaptiveTheme.colors.textMuted,
                                style = MaterialTheme.typography.body2,
                            )
                        }
                        if (conversation.unread > 0) AdaptiveBadge(
                            text = conversation.unread.coerceAtMost(99).toString(),
                            tone = AdaptiveBadgeTone.Info,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatDetail(
    state: OneChatView,
    onDraft: (String) -> Unit,
    onSend: (String, String) -> Unit,
    onBack: (() -> Unit)? = null,
) {
    val id = state.selectedId
    val conversation = state.conversations.find { it.id == id }
    var showProfile by remember(id) { mutableStateOf(false) }
    if (showProfile && conversation != null) {
        AlertDialog(
            onDismissRequest = { showProfile = false },
            title = { Text("Perfil del contacto", color = AdaptiveTheme.colors.textPrimary) },
            text = {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AdaptiveAvatar(name = conversation.title, size = 76.dp)
                    Text("@" + conversation.title, style = MaterialTheme.typography.h6,
                        color = AdaptiveTheme.colors.textPrimary)
                    Text("Contacto de OneChat", color = AdaptiveTheme.colors.textMuted,
                        style = MaterialTheme.typography.body2)
                    if (conversation.peerId.isNotBlank()) {
                        Text("ID de contacto: " + conversation.peerId,
                            color = AdaptiveTheme.colors.textMuted,
                            style = MaterialTheme.typography.caption)
                    }
                    Text("La presencia en línea aún no está disponible.",
                        color = AdaptiveTheme.colors.textSecondary,
                        style = MaterialTheme.typography.body2)
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfile = false }) { Text("Cerrar") }
            },
            backgroundColor = AdaptiveTheme.colors.surface,
            shape = RoundedCornerShape(20.dp),
        )
    }
    when {
        id == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AdaptiveEmptyState(
                title = "Elegí una conversación",
                description = "Tus mensajes aparecerán aquí.",
            )
        }
        conversation == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AdaptiveEmptyState("Esta conversación ya no está disponible")
        }
        else -> Column(Modifier.fillMaxSize().background(AdaptiveTheme.colors.background)) {
            Row(
                Modifier.fillMaxWidth().background(AdaptiveTheme.colors.surface)
                    .padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    AdaptiveIconButton(onClick = onBack, size = 44.dp) {
                        AdaptiveIcons.ChevronLeft(
                            size = 24.dp,
                            tint = AdaptiveTheme.colors.textPrimary,
                            contentDescription = "Volver a conversaciones",
                        )
                    }
                }
                Row(
                    modifier = Modifier.weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showProfile = true }
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AdaptiveAvatar(conversation.title, size = 40.dp)
                    Column(Modifier.weight(1f)) {
                    Text(conversation.title, fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.subtitle1,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        color = AdaptiveTheme.colors.textPrimary)
                    Text(
                        when (state.connection) {
                            ConnectionLabel.ONLINE -> "Conectado al servidor"
                            ConnectionLabel.CONNECTING -> "Conectando…"
                            ConnectionLabel.RECONNECTING -> "Reconectando…"
                            ConnectionLabel.OFFLINE -> "Sin conexión"
                        },
                        style = MaterialTheme.typography.caption,
                        color = if (state.connection == ConnectionLabel.ONLINE)
                            AdaptiveTheme.colors.success else AdaptiveTheme.colors.textMuted,
                    )
                }
                    }
                }
                StatusDot(state.connection)
            }
            Spacer(Modifier.height(1.dp).fillMaxWidth().background(AdaptiveTheme.colors.border))

            if (state.error != null) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AdaptiveTheme.colors.dangerSubtle).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(state.error, style = MaterialTheme.typography.body2,
                        color = AdaptiveTheme.colors.dangerText, modifier = Modifier.weight(1f))
                }
            }
            val visible = state.messages.filter { it.conversationId == id }
            val scrollState = rememberLazyListState()
            LaunchedEffect(id, visible.lastOrNull()?.id) {
                if (visible.isNotEmpty()) scrollState.animateScrollToItem(visible.lastIndex)
            }
            if (visible.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                    AdaptiveEmptyState(
                        title = "Todavía no hay mensajes",
                        description = "Mandá el primer mensaje para empezar.",
                    )
                }
            } else {
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    items(visible, key = { it.id }) { msg ->
                        MessageBubble(msg)
                    }
                }
            }
            MessageComposer(
                draft = state.draft,
                sending = state.sending,
                onDraft = onDraft,
                onSend = { onSend(id, state.draft.trim()) },
            )
        }
    }
}

@Composable
private fun StatusDot(connection: ConnectionLabel) {
    val color = when (connection) {
        ConnectionLabel.ONLINE -> AdaptiveTheme.colors.success
        ConnectionLabel.CONNECTING, ConnectionLabel.RECONNECTING -> AdaptiveTheme.colors.warning
        ConnectionLabel.OFFLINE -> AdaptiveTheme.colors.textMuted
    }
    Box(
        Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(color),
    )
}

@Composable
private fun MessageBubble(msg: OneChatBubble) {
    val shape = if (msg.mine)
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 5.dp)
    else
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 5.dp, bottomEnd = 18.dp)
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.mine) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 320.dp)
                .fillMaxWidth(0.81f)
                .clip(shape)
                .background(if (msg.mine) AdaptiveTheme.colors.primarySubtle else AdaptiveTheme.colors.surface)
                .border(
                    width = if (msg.mine) 0.dp else 1.dp,
                    color = if (msg.mine) Color.Transparent else AdaptiveTheme.colors.border,
                    shape = shape,
                )
                .padding(horizontal = 15.dp, vertical = 11.dp),
        ) {
            Text(
                msg.body,
                style = MaterialTheme.typography.body1,
                color = if (msg.mine) AdaptiveTheme.colors.primaryText else AdaptiveTheme.colors.textPrimary,
                lineHeight = 21.sp,
            )
            if (msg.mine) {
                Spacer(Modifier.height(3.dp))
                Text(
                    if (msg.readAt != null) "✓✓ Leído" else "✓ Enviado",
                    modifier = Modifier.align(Alignment.End),
                    style = MaterialTheme.typography.caption,
                    color = AdaptiveTheme.colors.textMuted,
                    fontSize = 10.sp,
                )
            }
        }
    }
}

private val quickEmojis = listOf(
    "😀", "😂", "🥹", "😊", "😍", "🥰", "😎",
    "😭", "😅", "😱", "🤔", "😴", "🙄", "😡",
    "❤️", "💙", "💚", "🔥", "✨", "🎉", "💯",
    "👍", "👎", "👏", "🙏", "🤝", "👀", "🐸",
)

/** Picker works on Android, Desktop and Wasm without uploading text or media. */
@Composable
private fun MessageComposer(
    draft: String,
    sending: Boolean,
    onDraft: (String) -> Unit,
    onSend: () -> Unit,
) {
    var showEmojis by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().background(AdaptiveTheme.colors.surface)
    ) {
        if (showEmojis) {
            Column(
                Modifier.fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(AdaptiveTheme.colors.surfaceMuted)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                quickEmojis.chunked(7).forEach { row ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        row.forEach { emoji ->
                            Box(
                                modifier = Modifier.size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        onDraft(draft + emoji)
                                        showEmojis = false
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(emoji, fontSize = 23.sp)
                            }
                        }
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            AdaptiveIconButton(onClick = { showEmojis = !showEmojis }, size = 44.dp) {
                SmileIcon(color = AdaptiveTheme.colors.textPrimary)
            }
            OutlinedTextField(
                value = draft,
                onValueChange = onDraft,
                modifier = Modifier.weight(1f).heightIn(min = 54.dp, max = 128.dp),
                placeholder = { Text("Escribí un mensaje") },
                singleLine = false,
                maxLines = 4,
                shape = RoundedCornerShape(20.dp),
                colors = oneChatFieldColors(),
                textStyle = MaterialTheme.typography.body1.copy(color = AdaptiveTheme.colors.textPrimary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
            )
            Button(
                onClick = onSend,
                enabled = draft.isNotBlank() && !sending,
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(17.dp),
                contentPadding = PaddingValues(0.dp),
                elevation = ButtonDefaults.elevation(defaultElevation = 0.dp),
            ) {
                if (sending) CircularProgressIndicator(
                    modifier = Modifier.size(19.dp),
                    color = MaterialTheme.colors.onPrimary,
                    strokeWidth = 2.dp,
                ) else SendArrow(color = MaterialTheme.colors.onPrimary)
            }
        }
    }
}

@Composable
private fun SmileIcon(color: Color) {
    Canvas(Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        drawCircle(color = color, radius = w * .43f, style = Stroke(width = w * .09f))
        drawCircle(color = color, radius = w * .045f,
            center = androidx.compose.ui.geometry.Offset(w * .37f, h * .41f))
        drawCircle(color = color, radius = w * .045f,
            center = androidx.compose.ui.geometry.Offset(w * .63f, h * .41f))
        drawArc(
            color = color, startAngle = 20f, sweepAngle = 140f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(w * .27f, h * .31f),
            size = androidx.compose.ui.geometry.Size(w * .46f, h * .44f),
            style = Stroke(width = w * .07f),
        )
    }
}

/** Scalable vector paper-plane icon; no Unicode fallback glyphs or font differences. */
@Composable
private fun SendArrow(color: Color) {
    Canvas(Modifier.size(23.dp)) {
        val w = size.width
        val h = size.height
        val plane = Path().apply {
            moveTo(w * 0.09f, h * 0.11f)
            lineTo(w * 0.93f, h * 0.50f)
            lineTo(w * 0.09f, h * 0.89f)
            lineTo(w * 0.26f, h * 0.55f)
            lineTo(w * 0.62f, h * 0.50f)
            lineTo(w * 0.26f, h * 0.45f)
            close()
        }
        drawPath(plane, color)
    }
}
