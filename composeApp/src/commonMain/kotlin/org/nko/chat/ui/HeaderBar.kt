package org.nko.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.nko.chat.models.Chat
import org.nko.chat.models.User
import org.nko.chat.services.BaseService
import org.nko.chat.util.BackHandler
import org.nko.chat.util.getSettings


@Composable
fun HeaderBar(
    user: User,
    selectedChat: Chat?,
    onBackClick: () -> Unit,
    onLogout: () -> Unit,
    onClickContacts: () -> Unit,
    showContacts: Boolean,
) {

    BackHandler(onBackPressed = onBackClick)


    val avatar = if(selectedChat != null) {
        if(selectedChat.isGroup) {
            selectedChat.avatar
        }else {
            if(selectedChat.createdBy?.id == user.id) {
                selectedChat.participants!![0].avatar
            }else {
                selectedChat.createdBy?.avatar
            }
        }
    }else{
        null
    }

    val chatName = if (selectedChat != null) {
        if (selectedChat.isGroup) {
            selectedChat.name
        } else {
            if (selectedChat.createdBy?.id == user.id) {
                selectedChat.participants!![0].name
            } else {
                selectedChat.createdBy?.name
            }
        }
    } else {
        null
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(6.dp)
    ) {
        val screenWidth = maxWidth


        Row (
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ){
             if (screenWidth > 600.dp || selectedChat == null) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.weight(3f)
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(Color.LightGray, CircleShape)
                        .clip(CircleShape)
                ){

                    AsyncImage(
                        model = "${getSettings().getStringOrNull("serverHost")}/${user.avatar?: "nofoto.webp"}",
                        contentDescription = "User Avatar",
                        modifier = Modifier.size(50.dp),
                        contentScale = ContentScale.Crop
                    )

                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    if(!showContacts) {
                        Button(
                            onClick = {
                                onClickContacts()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        ) {
                            Icon(
                                Icons.Filled.Group,
                                contentDescription = "Contacts",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        DropdownMenuComponent(
                            options = listOf(
                                "Perfil" to { /* Acción para Perfil */ },
                                "Configuración" to { /* Acción para Configuración */ },
                                "Cerrar Sesión" to onLogout
                            )
                        )
                    }
                }
            }

            }

             if (screenWidth > 600.dp || selectedChat != null) {


            Row(
                modifier = Modifier
                    .weight(7f)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (screenWidth < 600.dp) {
                        IconButton(
                            modifier = Modifier.size(30.dp),
                            onClick = { onBackClick() }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }

                    if (selectedChat != null) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.LightGray, CircleShape)
                                .clip(CircleShape)
                        ){
                            AsyncImage(
                                model = "${BaseService()._getServerHost()}/${avatar ?: "nofoto.webp"}",
                                contentDescription = "Profile Picture",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = chatName ?: "Chat App",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedChat != null) {
                            Row {
                                if (selectedChat.isGroup) {
                                    selectedChat.participants!!.forEachIndexed  {index, participant ->
                                        Text(
                                            text = if (index == selectedChat.participants.size - 1) participant.name!! else "${participant.name}, ",
                                            fontSize = 12.sp,
                                            color = Color.LightGray,
                                            maxLines = 1
                                        )
                                    }
                                }else{
                                    Text(
                                        text = "Online" ,
                                        fontSize = 12.sp,
                                        color = Color.LightGray
                                    )
                                }
                            }

                        }
                    }

                }

                if (selectedChat != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            modifier = Modifier.size(30.dp),
                            onClick = { }
                        ) {
                            Icon(Icons.Filled.Call, contentDescription = "Call")
                        }
                        DropdownMenuComponent(
                            options = listOf(
                                "Perfil" to { /* Acción para Perfil */ },
                                "Configuración" to { /* Acción para Configuración */ },
                                "Cerrar Sesión" to { /* Acción para Cerrar Sesión */ }
                            )
                        )
                    }
                }
            }


            }
        }



    }
}

@Composable
fun DropdownMenuComponent(options: List<Pair<String, () -> Unit>>) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
        ) {
            options.forEach { option ->
                TextButton(onClick = {
                    expanded = false
                    option.second()
                }) {
                    Text(
                        option.first,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
