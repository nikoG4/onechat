package org.nko.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModel
import org.nko.chat.models.Chat
import org.nko.chat.models.Message
import org.nko.chat.models.User
import org.nko.chat.services.AuthService
import org.nko.chat.services.ContactService

@Composable
fun ChatSidebar(
    chats: List<Chat>,
    selectedChat: Chat?,
    onSelectChat: (Chat) -> Unit,
    onFilterChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var filter by remember { mutableStateOf("all") }
    var searchQuery by remember { mutableStateOf("") }
    var showNonContacts by remember { mutableStateOf(false) }

    val user: User = AuthService().getUserLocally()!!

    val contacts = remember(searchQuery) {
        ContactService().getContacts(true, searchQuery)
    }

    var nonContacts by remember { mutableStateOf<List<User>>(emptyList()) }

    LaunchedEffect(Unit){
        nonContacts = ContactService().getNonContacts(searchQuery).getOrNull() ?: emptyList()
    }


    val chatNames = chats.map { it.name }.toSet()

    val filteredChats = if (searchQuery.isEmpty()) {
        chats
    } else {
        chats.filter { chat ->
            chat.name!!.contains(searchQuery, ignoreCase = true)
        }
    }
    val filteredContacts = contacts.filter { !chatNames.contains(it.name) }
    val filteredNonContacts = nonContacts.filter { !chatNames.contains(it.name) }

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxHeight()
            .background(color = MaterialTheme.colorScheme.background)

    ) {

        // Barra de búsqueda
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                shape = RoundedCornerShape(30.dp),
                singleLine = true,
                placeholder = { Text("Buscar...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(
                        shape = CircleShape, // Bordes redondeados
                        color = Color.LightGray
                    ),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }

        // Filtros
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterButton("Todos", filter == "all") {
                filter = "all"
                onFilterChange("all")
            }
            FilterButton("No leídos", filter == "unread") {
                filter = "unread"
                onFilterChange("unread")
            }
            FilterButton("Grupos", filter == "groups") {
                filter = "groups"
                onFilterChange("groups")
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // Mostrar chats filtrados
            if (searchQuery.isNotEmpty() && filteredChats.isNotEmpty()) {
                item {
                    Text(
                        text = "Chats",
                        color = Color(0xff636363),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
            items(filteredChats) { chat ->
                ChatListItem(
                    chat = chat,
                    selected = selectedChat?.id == chat.id,
                    lastMessage = getLastMessage(chat),
                    onClick = { onSelectChat(chat) }
                )
            }

            // Mostrar contactos
            if (searchQuery.isNotEmpty() && filteredContacts.isNotEmpty()) {
                item {
                    Text(
                        text = "Tus contactos",
                        color = Color(0xff636363),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp, start = 8.dp)
                    )
                }
                items(filteredContacts) { contact ->
                    val newChat = Chat(
                        id = null,
                        name = contact.name,
                        isGroup = false,
                        avatar = contact.avatar,
                        messages = emptyList(),
                        participants = listOf(contact, user)
                    )
                    ChatListItem(
                        chat = newChat,
                        selected = false,
                        lastMessage = null,
                        onClick = { onSelectChat(newChat) }
                    )
                }
            }

            // Mostrar texto para descubrir nuevos usuarios
            if (searchQuery.isNotEmpty() && filteredNonContacts.isNotEmpty() && !showNonContacts) {
                item {
                    Text(
                        text = "Descubrir nuevos usuarios",
                        color = Color(0xFF0066CC),  // Color azul
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,  // Tamaño de fuente
                        modifier = Modifier
                            .padding(top = 16.dp, start = 8.dp)
                            .clickable { showNonContacts = true }
                            .background(
                                color = Color(0xFFE0F7FA),  // Fondo suave
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )

                }
            }

            // Mostrar no contactos si showNonContacts es verdadero
            if (showNonContacts && searchQuery.isNotEmpty() && filteredNonContacts.isNotEmpty()) {
                item {
                    Text(
                        text = "Otros usuarios",
                        color = Color(0xff636363),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp, start = 8.dp)
                    )
                }
                items(filteredNonContacts) { contact ->
                    val newChat = Chat(
                        id = null,
                        name = contact.name,
                        isGroup = false,
                        avatar = contact.avatar,
                        messages = emptyList(),
                        participants = listOf(contact, user)
                    )
                    ChatListItem(
                        chat = newChat,
                        selected = false,
                        lastMessage = null,
                        onClick = { onSelectChat(newChat) }
                    )
                }
            }
        }

    }
}


@Composable
fun FilterButton(label: String, isActive: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = Color(0xff636363),
        fontWeight = FontWeight.Bold ,
        modifier = Modifier.clickable(onClick = onClick).background(
            if (isActive) Color.LightGray else Color.Gray,
            shape = CircleShape
        ).padding(
            horizontal = 8.dp,
            vertical = 4.dp
        )

    )
}

fun getLastMessage(chat: Chat): Message? {
    if (chat.messages?.isEmpty() == true) return null

    val sortedMessages = chat.messages?.sortedByDescending { it.date }
    if (sortedMessages != null) {
        return sortedMessages.first()
    }
    return null

}

