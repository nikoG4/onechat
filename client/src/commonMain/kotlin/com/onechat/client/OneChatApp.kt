package com.onechat.client

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.adaptivekt.components.AdaptiveAvatar
import io.github.adaptivekt.components.AdaptiveIconButton
import io.github.adaptivekt.components.icons.AdaptiveIcons
import io.github.adaptivekt.core.AdaptiveColorSchemes
import io.github.adaptivekt.core.AdaptiveTheme
import com.onechat.validation.OneChatMavenWorkspace

/**
 * AdaptiveKT owns the semantic palette and follows the device's theme.
 * Material 2 controls use the same semantic colors until equivalents ship via Maven Central.
 */
@Composable
fun OneChatApp() {
    val scope = rememberCoroutineScope()
    val controller = remember(scope) { OneChatController(OneChatApi(), scope) }
    DisposableEffect(controller) { onDispose { controller.close() } }
    val dark = isSystemInDarkTheme()
    AdaptiveTheme(colorScheme = if (dark) AdaptiveColorSchemes.defaultDark() else AdaptiveColorSchemes.defaultLight()) {
        val palette = AdaptiveTheme.colors
        val materialColors = if (dark) darkColors(
            primary = palette.primary,
            primaryVariant = palette.primary,
            secondary = palette.info,
            background = palette.background,
            surface = palette.surface,
            onPrimary = palette.textInverse,
            onSecondary = palette.textInverse,
            onBackground = palette.textPrimary,
            onSurface = palette.textPrimary,
            error = palette.danger,
        ) else lightColors(
            primary = palette.primary,
            primaryVariant = palette.primary,
            secondary = palette.info,
            background = palette.background,
            surface = palette.surface,
            onPrimary = palette.textInverse,
            onSecondary = palette.textInverse,
            onBackground = palette.textPrimary,
            onSurface = palette.textPrimary,
            error = palette.danger,
        )
        MaterialTheme(colors = materialColors) {
            val user = controller.account
            OneChatBackHandler(
                enabled = user != null && (controller.newConversation || controller.view.selectedId != null),
                onBack = {
                    if (controller.newConversation) controller.dismissNewConversation()
                    else controller.back()
                },
            )
            Box(Modifier.fillMaxSize().background(AdaptiveTheme.colors.background)) {
                when {
                    user == null -> AuthScreen(controller)
                    controller.newConversation -> NewConversationScreen(controller)
                    else -> Column(Modifier.fillMaxSize()) {
                        AppHeader(username = user.username, onSignOut = controller::signOut)
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
    }
}

@Composable
private fun AppHeader(username: String, onSignOut: () -> Unit) {
    Column {
        Row(
            Modifier.fillMaxWidth()
                .background(AdaptiveTheme.colors.surface)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(38.dp).clip(RoundedCornerShape(13.dp))
                        .background(AdaptiveTheme.colors.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("O", fontWeight = FontWeight.Bold, fontSize = 22.sp,
                        color = AdaptiveTheme.colors.textInverse)
                }
                Spacer(Modifier.width(11.dp))
                Column {
                    Text("OneChat", style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Bold, color = AdaptiveTheme.colors.textPrimary)
                    Text("@" + username, style = MaterialTheme.typography.caption,
                        color = AdaptiveTheme.colors.textMuted)
                }
            }
            TextButton(onClick = onSignOut, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text("Salir", style = MaterialTheme.typography.button)
            }
        }
        Spacer(Modifier.fillMaxWidth().height(1.dp).background(AdaptiveTheme.colors.border))
    }
}

@Composable
private fun AuthScreen(controller: OneChatController) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var invite by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(AdaptiveTheme.colors.background).padding(16.dp),
        contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 440.dp).fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(AdaptiveTheme.colors.surface)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 26.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(18.dp))
                    .background(AdaptiveTheme.colors.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text("O", color = AdaptiveTheme.colors.textInverse,
                    style = MaterialTheme.typography.h4, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (creating) "Creá tu cuenta" else "Bienvenido a OneChat",
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.Bold,
                color = AdaptiveTheme.colors.textPrimary,
            )
            Text(
                if (creating) "Registrate para empezar a conversar."
                else "Tus conversaciones, en todos tus dispositivos.",
                style = MaterialTheme.typography.body2,
                color = AdaptiveTheme.colors.textMuted,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it.take(24) },
                label = { Text("Usuario") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = oneChatFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            if (creating && username.isNotBlank() && !SignupValidation.validUsername(username)) {
                Text("Usá de 3 a 24 caracteres, empezando con una letra.",
                    color = AdaptiveTheme.colors.danger, style = MaterialTheme.typography.caption)
            }
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = oneChatFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            if (creating && password.isNotEmpty() && !SignupValidation.validPassword(password)) {
                Text(
                    "Se necesitan al menos 8 bytes UTF-8. Ahora: " + SignupValidation.passwordBytes(password) + ".",
                    color = AdaptiveTheme.colors.danger, style = MaterialTheme.typography.caption,
                )
            }
            if (creating) {
                OutlinedTextField(
                    value = invite,
                    onValueChange = { invite = it },
                    label = { Text("Código de invitación") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = oneChatFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            controller.authError?.let {
                Text(it, color = AdaptiveTheme.colors.danger,
                    style = MaterialTheme.typography.body2)
            }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { controller.authenticate(username, password, if (creating) invite else null, creating) },
                enabled = !controller.authBusy && if (creating)
                    SignupValidation.error(username, password, invite) == null
                else username.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                elevation = ButtonDefaults.elevation(defaultElevation = 0.dp),
            ) {
                if (controller.authBusy) CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colors.onPrimary,
                    strokeWidth = 2.dp,
                ) else Text(if (creating) "Crear cuenta" else "Iniciar sesión",
                    fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = {
                creating = !creating
            }, modifier = Modifier.fillMaxWidth()) {
                Text(if (creating) "Ya tengo una cuenta" else "Crear una cuenta")
            }
            OutlinedButton(
                onClick = controller::checkServer,
                enabled = !controller.serverBusy && !controller.authBusy,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(if (controller.serverBusy) "Verificando…" else "Comprobar conexión")
            }
            controller.serverStatus?.let { status ->
                Text(status, color = AdaptiveTheme.colors.textSecondary,
                    style = MaterialTheme.typography.caption)
            }
            Text(
                "Render Free puede necesitar unos segundos para activarse.",
                style = MaterialTheme.typography.caption,
                color = AdaptiveTheme.colors.textMuted,
            )
        }
    }
}

@Composable
private fun NewConversationScreen(controller: OneChatController) {
    Column(Modifier.fillMaxSize().background(AdaptiveTheme.colors.background)) {
        Row(
            Modifier.fillMaxWidth().background(AdaptiveTheme.colors.surface)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AdaptiveIconButton(onClick = controller::dismissNewConversation, size = 44.dp) {
                AdaptiveIcons.ChevronLeft(
                    size = 24.dp,
                    tint = AdaptiveTheme.colors.textPrimary,
                    contentDescription = "Volver",
                )
            }
            Text("Nueva conversación", color = AdaptiveTheme.colors.textPrimary,
                style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Encontrá un contacto por su usuario de OneChat.",
                style = MaterialTheme.typography.body2, color = AdaptiveTheme.colors.textMuted)
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = controller.recipientQuery,
                    onValueChange = controller::recipientQuery,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = oneChatFieldColors(),
                    placeholder = { Text("Buscar usuario") },
                )
                Button(
                    onClick = controller::findRecipients,
                    enabled = controller.recipientQuery.isNotBlank() && !controller.findingUser,
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.elevation(defaultElevation = 0.dp),
                ) { Text("Buscar") }
            }
            controller.view.error?.let { Text(it, color = AdaptiveTheme.colors.danger) }
            if (controller.findingUser) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (!controller.findingUser && controller.matchingUsers.isEmpty()) {
                Text("Los usuarios registrados aparecerán aquí.",
                    style = MaterialTheme.typography.body2,
                    color = AdaptiveTheme.colors.textMuted)
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(controller.matchingUsers, key = { it.id }) { user ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(AdaptiveTheme.colors.surface).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AdaptiveAvatar(user.username, size = 42.dp)
                        Text("@" + user.username,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = AdaptiveTheme.colors.textPrimary,
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.SemiBold)
                        Button(onClick = { controller.startConversation(user) },
                            shape = RoundedCornerShape(12.dp)) { Text("Chat") }
                    }
                }
            }
        }
    }
}
