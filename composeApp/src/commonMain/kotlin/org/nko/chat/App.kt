package org.nko.chat

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.nko.chat.services.AuthService
import org.nko.chat.ui.MainScreen
import org.nko.chat.ui.GoogleLoginScreen
import org.nko.chat.util.getSettings
import kotlin.system.exitProcess

@Composable
fun ChatApp(driverFactory: org.nko.chat.util.DriverFactory) {
    // Inicializar base de datos
    org.nko.chat.services.DatabaseManager.init(driverFactory)

    // Inicializar Firebase
    remember {
        try {
            Firebase.initialize(options = org.nko.chat.services.FirebaseManager.options)
        } catch (e: Exception) {
            // Ya inicializado o error de configuración
        }
        true
    }

    val userRepository = remember { org.nko.chat.services.UserRepository() }
    val currentUser by userRepository.currentUserFlow.collectAsState(initial = null)
    
    var currentScreen by remember { mutableStateOf("loading") }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(currentUser) {
        currentScreen = if (currentUser != null) "main" else "google"
    }

    when (currentScreen) {
        "loading" -> {
            CircularProgressIndicator()
        }
        "google" -> GoogleLoginScreen(
            onSignIn = { idToken ->
                coroutineScope.launch {
                    val signedUser = userRepository.signInWithGoogleCredential(idToken)
                    if (signedUser == null) {
                        // Show error
                    }
                }
            }
        )
        "main" -> currentUser?.let {
            MainScreen(
                currentUser = it,
                onLogout = {
                    coroutineScope.launch { userRepository.signOut() }
                }
            )
        }
    }
}
