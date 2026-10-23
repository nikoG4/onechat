package com.onechat.client

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

private const val CHANNEL_ID = "onechat_private_messages"

private fun hasPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < 33 ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

internal class AndroidIncomingNotifier(private val context: Context) : IncomingNotifier {
    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun show(title: String, message: String, conversationId: String) {
        if (!hasPermission(context)) return
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Mensajes de OneChat",
                    NotificationManager.IMPORTANCE_DEFAULT).apply {
                        description = "Mensajes recibidos mientras OneChat mantiene conexión"
                        lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                    }
            )
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "com.onechat.client.OPEN_CHAT"
            putExtra("conversation_id", conversationId)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, conversationId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = if (Build.VERSION.SDK_INT >= 26)
            Notification.Builder(context, CHANNEL_ID)
        else Notification.Builder(context)
        val notification = builder
            .setSmallIcon(com.onechat.client.R.drawable.ic_onechat_notification)
            .setContentTitle("Mensaje de @" + title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setCategory(Notification.CATEGORY_MESSAGE)
            .build()
        manager.notify(conversationId.hashCode(), notification)
    }
}

@Composable
actual fun rememberIncomingNotifier(): IncomingNotifier {
    val context = LocalContext.current.applicationContext
    return remember(context) { AndroidIncomingNotifier(context) }
}

@Composable
actual fun NotificationPermissionPrompt() {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasPermission(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
    }
    if (!granted) {
        OutlinedButton(onClick = {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }) {
            Text("Activar alertas")
        }
    }
}
