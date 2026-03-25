package org.nko.chat.ui


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.nko.chat.models.Chat
import org.nko.chat.models.Message
import org.nko.chat.models.User
import org.nko.chat.services.AuthService
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun ChatContent(
    chat: Chat?,
    modifier: Modifier = Modifier,
    onSendMessage: (Message) -> Unit = { _ -> }
) {
    val user = AuthService().getUserLocally()


    // Si no hay chat, simplemente no mostramos nada
    if (chat == null) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)

        ) {
            Icon(
                imageVector = Icons.Default.ChatBubbleOutline,
                contentDescription = "Search Icon",
                tint = MaterialTheme.colorScheme.onBackground
            )
           Box(modifier = Modifier ) {
               Text("Selecciona un chat para empezar.")
           }
        }

    }else{
        Column(
            modifier = modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Sección de mensajes
            Box(modifier = Modifier.weight(1f)) {
                MessageList(
                    messages = chat.messages ?: emptyList(),
                    isGroup = chat.isGroup
                )
            }

            // Sección de entrada de chat
            ChatInput(onSendMessage = { messageContent ->
                val newMessage = Message(
                    id = null,
                    message = messageContent,
                    date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")),
                    user = User(id = user?.id),
                    status = "pending",
                    type = "text",
                    media = null,
                    chat = chat,
                    recipients = chat.participants?.map { User(id = it.id) }
                )

                // Notifica al nivel superior
                onSendMessage(newMessage)
            })
        }
    }


}


