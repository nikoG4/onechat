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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.nko.chat.models.Chat
import org.nko.chat.models.User
import org.nko.chat.services.ContactService
import org.nko.chat.util.BackHandler

@Composable
fun ContactList(
    user: User,
    onSelectContact: (User) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBackPressed = onBackClick)

    var searchQuery by remember { mutableStateOf("") }
    var showNonContacts by remember { mutableStateOf(false) }

    val contacts = remember(searchQuery) {
        ContactService().getContacts(true, searchQuery)
    }

    var nonContacts by remember { mutableStateOf<List<User>>(emptyList()) }

    LaunchedEffect(Unit){
        nonContacts = ContactService().getNonContacts(searchQuery).getOrNull() ?: emptyList()
    }


    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(color = MaterialTheme.colorScheme.background)
    ) {
        // Barra de búsqueda
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {

            IconButton(
                modifier = Modifier.size(30.dp),
                onClick = { onBackClick() }
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }

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
                    placeholder = { Text("Buscar contacto...") },
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
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {

            // Mostrar contactos
            item {
                Text(
                    text = "Tus contactos",
                    color = Color(0xff636363),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, start = 8.dp)
                )
            }
            items(contacts) { contact ->
                val newChat = Chat(
                    id = null,
                    name = contact.name,
                    isGroup = false,
                    avatar = contact.avatar,
                    messages = emptyList(),
                    participants = listOf(contact),
                    createdBy = contact
                )
                ChatListItem(
                    chat = newChat,
                    selected = false,
                    lastMessage = null,
                    onClick = { onSelectContact(contact) }
                )
            }


            // Mostrar texto para descubrir nuevos usuarios
            if (searchQuery.isNotEmpty() && nonContacts.isNotEmpty() && !showNonContacts) {
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
            if (showNonContacts && searchQuery.isNotEmpty() && nonContacts.isNotEmpty()) {
                item {
                    Text(
                        text = "Otros usuarios",
                        color = Color(0xff636363),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp, start = 8.dp)
                    )
                }
                items(nonContacts) { contact ->
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
                        onClick = { onSelectContact(contact) }
                    )
                }
            }
        }
    }
}