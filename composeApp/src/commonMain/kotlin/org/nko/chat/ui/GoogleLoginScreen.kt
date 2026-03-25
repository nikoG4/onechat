package org.nko.chat.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp

@Composable
fun GoogleLoginScreen(
    onSignIn: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var idToken by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Text(
            text = "Chat KMP + Firebase",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Manual ID Token for Desktop development, or trigger native flow
        OutlinedTextField(
            value = idToken,
            onValueChange = { idToken = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Google ID Token") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                onSignIn(idToken)
            },
            enabled = idToken.isNotBlank() && !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLoading) "Cargando..." else "Iniciar Sesión con Google")
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = errorMessage!!, color = androidx.compose.ui.graphics.Color.Red)
        }
    }
}
}

