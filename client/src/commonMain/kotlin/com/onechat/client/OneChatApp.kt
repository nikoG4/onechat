package com.onechat.client

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onechat.validation.OneChatMavenWorkspace

/** Actual Android/Desktop/Web client of the deployed OneChat Ktor API. No fake chats. */
@Composable
fun OneChatApp() {
    val scope = rememberCoroutineScope()
    val controller = remember(scope) { OneChatController(OneChatApi(), scope) }
    DisposableEffect(controller) { onDispose { controller.close() } }
    MaterialTheme {
        val user = controller.account
        when {
            user == null -> AuthScreen(controller)
            controller.newConversation -> NewConversationScreen(controller)
            else -> Column(Modifier.fillMaxSize()) {
                Surface(elevation = 2.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("OneChat", style = MaterialTheme.typography.h6, fontWeight = FontWeight.SemiBold)
                            Text("@" + user.username, style = MaterialTheme.typography.caption)
                        }
                        TextButton(onClick = controller::signOut) { Text("Cerrar sesión") }
                    }
                }
                Box(Modifier.weight(1f)) {
                    OneChatMavenWorkspace(
                        state = controller.view,
                        onSelect = controller::select,
                        onBack = controller::back,
                        onQuery = controller::query,
                        onDraft = controller::draft,
                        onSend = controller::send,
                        onCreateConversation = controller::showNewConversation,
                        onRetry = controller::refresh,
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthScreen(controller: OneChatController) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var invite by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colors.background), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth().widthIn(max = 450.dp).verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 26.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("OneChat", style = MaterialTheme.typography.h4, fontWeight = FontWeight.Bold)
            Text(
                if (creating) "Creá tu cuenta para empezar a conversar."
                else "Tus conversaciones, en Android, escritorio y navegador.",
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(5.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it.take(24) },
                label = { Text("Usuario") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (creating) {
                OutlinedTextField(
                    value = invite,
                    onValueChange = { invite = it },
                    label = { Text("Código de invitación") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (controller.authError != null) {
                Text(
                    controller.authError.orEmpty(),
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.body2,
                )
            }
            Button(
                onClick = {
                    controller.authenticate(username, password, if (creating) invite else null, creating)
                },
                enabled = !controller.authBusy && username.isNotBlank() &&
                    password.isNotBlank() && (!creating || invite.isNotBlank()),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                if (controller.authBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else Text(if (creating) "Crear cuenta" else "Entrar")
            }
            TextButton(onClick = { creating = !creating }) {
                Text(if (creating) "Ya tengo una cuenta" else "Crear una cuenta con invitación")
            }
            OutlinedButton(
                onClick = controller::checkServer,
                enabled = !controller.serverBusy && !controller.authBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (controller.serverBusy) "Verificando servidor…" else "Probar conexión con el servidor")
            }
            controller.serverStatus?.let { status ->
                Text(
                    status,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.75f),
                )
            }
            Text(
                "Servidor: Render Free. La primera conexión después de inactividad puede tardar.",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun NewConversationScreen(controller: OneChatController) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = controller::dismissNewConversation) { Text("Volver") }
            Spacer(Modifier.width(12.dp))
            Text("Nueva conversación", style = MaterialTheme.typography.h6)
        }
        Text("Buscá otro usuario registrado en OneChat.", style = MaterialTheme.typography.body2)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = controller.recipientQuery,
                onValueChange = controller::recipientQuery,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Nombre de usuario") },
            )
            Button(
                onClick = controller::findRecipients,
                enabled = controller.recipientQuery.isNotBlank() && !controller.findingUser,
            ) {
                Text("Buscar")
            }
        }
        controller.view.error?.let { Text(it, color = MaterialTheme.colors.error) }
        if (controller.findingUser) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (!controller.findingUser && controller.matchingUsers.isEmpty()) {
            Text(
                "Si no hay resultados, verificá que tu contacto ya haya creado una cuenta.",
                style = MaterialTheme.typography.caption,
            )
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(controller.matchingUsers, key = { it.id }) { user ->
                Card(elevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("@" + user.username, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f))
                        Button(onClick = { controller.startConversation(user) }) { Text("Conversar") }
                    }
                }
            }
        }
    }
}
