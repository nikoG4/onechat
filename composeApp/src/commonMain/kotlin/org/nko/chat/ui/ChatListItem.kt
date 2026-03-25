package org.nko.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.nko.chat.models.Chat
import org.nko.chat.models.Message
import org.nko.chat.services.AuthService
import org.nko.chat.services.BaseService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ChatListItem(
    chat: Chat,
    selected: Boolean,
    lastMessage: Message?,

    onClick: () -> Unit
) {

    val user = AuthService().getUserLocally()

    val time = lastMessage?.date
    val lastMessageStatus = lastMessage?.status

    val timeFormatted = time?.let {
        val msgDateTime = LocalDateTime.parse(it, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
        val msgDate = msgDateTime.toLocalDate()

        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

         when {
            msgDate == today -> msgDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
            msgDate == yesterday -> "AYER"
            msgDate.isAfter(today.minusDays(7)) -> msgDate.dayOfWeek
                .getDisplayName(TextStyle.FULL, Locale("es", "ES"))
                .uppercase()
            else -> msgDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        }
    }

    val avatar = if(chat.isGroup || chat.id == null) {
        chat.avatar
    }else {
        if(chat.createdBy?.id == user?.id) {
            chat.participants!![0].avatar
        }else {
            chat.createdBy?.avatar
        }

    }

    val chatName = if(chat.isGroup || chat.id == null) {
        chat.name
    }else {
        if(chat.createdBy?.id == user?.id) {
            chat.participants!![0].name
        }else {
            chat.createdBy?.name
        }

    }


    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) Color.LightGray else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(40.dp)
                .background(if (selected) Color.White else Color.LightGray, CircleShape)
                .clip(CircleShape)
        ){
            AsyncImage(
                model = "${BaseService()._getServerHost()}/${avatar ?: "nofoto.webp"}",
                contentDescription = "Profile Picture",
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chatName!!,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
            ) {
                if(lastMessage?.user?.id == user?.id){
                    when (lastMessageStatus) {
                        "sent" -> Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(14.sp.value.dp)
                        )
                        "received" -> Icon(
                            Icons.Filled.DoneAll,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(14.sp.value.dp)
                        )
                        "viewed" -> Icon(
                            Icons.Default.DoneAll,
                            contentDescription = null,
                            tint = Color(0xff53bdeb),
                            modifier = Modifier.size(14.sp.value.dp)
                        )
                        else -> Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(14.sp.value.dp)
                        )

                    }
                    Spacer(modifier = Modifier.width(3.dp))
                }



                lastMessage?.message?.let {
                     val lastMessageText = if (chat.isGroup) {
                         "${if(lastMessage.user.id == user?.id) "Tú" else lastMessage.user.name}: $it "
                     }else{
                         it
                     }

                    Text(
                        text = lastMessageText,
                        color = Color.Gray,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }

        }
        timeFormatted?.let {
            Text(
                text = it,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}
