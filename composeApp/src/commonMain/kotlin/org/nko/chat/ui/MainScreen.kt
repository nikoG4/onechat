package org.nko.chat.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.nko.chat.models.Chat
import org.nko.chat.models.Message
import org.nko.chat.models.User

@Composable
fun MainScreen(currentUser: User, onLogout: () -> Unit) {

    var selectedChat by remember { mutableStateOf<Chat?>(null) }
    var filter by remember { mutableStateOf("all") }
    var showContacts by remember { mutableStateOf(false) }
    
    val chatRepository = remember { org.nko.chat.services.ChatRepository() }
    val contactRepository = remember { org.nko.chat.services.ContactRepository() }
    
    val chats by chatRepository.localChatsFlow.collectAsState(initial = emptyList())
    val messages by if (selectedChat?.id != null) {
        chatRepository.getLocalMessagesFlow(selectedChat!!.id!!).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList<Message>()) }
    }

    val coroutineScope = rememberCoroutineScope()

    // Sincronización automática de mensajes para el chat seleccionado
    LaunchedEffect(selectedChat?.id) {
        selectedChat?.id?.let {
            chatRepository.syncChat(it)
        }
    }

    val filteredChats = chats.filter { chat ->
        when (filter) {
            "groups" -> chat.isGroup
            else -> true
        }
    }

    fun findChatWithUser(user: User): Chat? {
        return chats.find {
            it.id == user.id // Simplified: assuming direct chat ID matches user ID or needs mapping
        }
    }

    val sendMessage = { message: Message ->
        val chatId = selectedChat?.id ?: message.recipients?.firstOrNull()?.id
        if (chatId != null) {
            coroutineScope.launch {
                chatRepository.sendMessage(
                    chatId = chatId,
                    senderId = currentUser.id ?: "",
                    text = message.message
                )
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isCompact = maxWidth < 600.dp

        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            HeaderBar(
                user = currentUser,
                selectedChat = selectedChat,
                onBackClick = {
                    if (selectedChat != null) {
                        selectedChat = null
                        showContacts = false
                    }
                },
                onLogout = onLogout,
                onClickContacts = { showContacts = !showContacts },
                showContacts = showContacts
            )
            
            if (isCompact) {
                if (selectedChat != null) {
                    ChatContent(
                        chat = selectedChat!!.copy(messages = messages),
                        modifier = Modifier.fillMaxSize(),
                        onSendMessage = { message -> sendMessage(message) }
                    )
                } else if (showContacts) {
                    ContactList(
                        user = currentUser,
                        onSelectContact = { contact ->
                            selectedChat = findChatWithUser(contact) ?: Chat(
                                id = contact.id, // Direct chat with user
                                name = contact.name,
                                participants = listOf(contact)
                            )
                            showContacts = false
                        },
                        onBackClick = { showContacts = false },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    ChatSidebar(
                        chats = filteredChats,
                        selectedChat = selectedChat,
                        onSelectChat = { selectedChat = it },
                        onFilterChange = { filter = it },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (showContacts) {
                        ContactList(
                            user = currentUser,
                            onSelectContact = { contact ->
                                selectedChat = findChatWithUser(contact) ?: Chat(
                                    id = contact.id,
                                    name = contact.name,
                                    participants = listOf(contact)
                                )
                            },
                            onBackClick = { showContacts = false },
                            modifier = Modifier.weight(3f)
                        )
                    } else {
                        ChatSidebar(
                            chats = filteredChats,
                            selectedChat = selectedChat,
                            onSelectChat = { selectedChat = it },
                            onFilterChange = { filter = it },
                            modifier = Modifier.weight(3f)
                        )
                    }

                    ChatContent(
                        chat = selectedChat?.copy(messages = messages),
                        modifier = Modifier.weight(7f),
                        onSendMessage = { message -> sendMessage(message) }
                    )
                }
            }
        }
    }
}

}