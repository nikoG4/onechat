package org.nko.chat.ui


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
fun ChatInput(onSendMessage: (String) -> Unit) {
    var message by remember { mutableStateOf("") }

    // Para gestionar la visibilidad del panel de emojis y adjuntos
    val handleSend = {
        if (message.trim().isNotEmpty()) {
            onSendMessage(message)
            message = "" // Limpia el campo de entrada
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            TextField(
                value = message,
                onValueChange = { message = it },
                leadingIcon = { Icon(Icons.Filled.Mood, contentDescription = "Emoji") },
                trailingIcon = { Icon(Icons.Default.AttachFile, contentDescription = "Adjuntos") },
                shape = RoundedCornerShape(30.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(70.dp)
                    .padding(8.dp)
                    .background(Color.Transparent),
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        handleSend()
                    }
                ),
                singleLine = false,
                placeholder = { Text("Escribe un mensaje...") },
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            // Botón de enviar o micrófono
            if (message.trim().isNotEmpty()) {
                IconButton(
                    onClick = { handleSend() },
                    modifier = Modifier.background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.extraLarge
                    )
                ){
                    Icon(
                        Icons.AutoMirrored.Default.Send,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.onPrimary

                    )
                }
            } else {
                IconButton(
                    onClick = {},
                    modifier = Modifier.background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.extraLarge
                    )
                ){
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Mic",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}