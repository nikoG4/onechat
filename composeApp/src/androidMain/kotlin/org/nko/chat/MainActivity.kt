package org.nko.chat

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview


class MainActivity : ComponentActivity() {

    init {
        instance = this
    }

    companion object {
        private var instance: MainActivity? = null
        val appContext: Context
            get() = instance!!.applicationContext
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isDarkTheme = resources.configuration.uiMode.and(0x30) == 0x20

        setContent {
            ChatAppTheme(isDarkTheme = isDarkTheme) {
                ChatApp(org.nko.chat.util.DriverFactory(applicationContext))
            }
        }
    }
}

@Composable
fun ChatAppTheme(isDarkTheme: Boolean, content: @Composable () -> Unit) {
    val colors = if (isDarkTheme) {
        darkColorScheme() // Usa el esquema de colores para el modo oscuro
    } else {
        lightColorScheme() // Usa el esquema de colores para el modo claro
    }

    MaterialTheme(
        colorScheme = colors // Establece el esquema de colores
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background), // Usa el color de fondo del MaterialTheme
            color = MaterialTheme.colorScheme.background // Asigna el color de fondo automáticamente
        ) {
            content() // Contenido de la app
        }
    }
}




@Preview
@Composable
fun AppAndroidPreview() {
        ChatApp()
}