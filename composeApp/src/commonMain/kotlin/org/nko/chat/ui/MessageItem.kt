package org.nko.chat.ui


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.nko.chat.models.Message
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun MessageItem(
    message: Message,
    isSender: Boolean,
    isGroup: Boolean,
    isConsecutive: Boolean
) {


    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 8.dp,
                end = 8.dp,
                top = if (isConsecutive) 2.dp else 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isSender) Arrangement.End else Arrangement.Start
    ) {
        if (isGroup && !isSender) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .padding(3.dp)
                    .background(Color.LightGray, CircleShape)
                    .clip(CircleShape)
            ){
                AsyncImage(
                    model = message.user.avatar,
                    contentDescription = "Sender Avatar",
                    modifier = Modifier
                        .size(30.dp),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Column(
            modifier = Modifier
                .wrapContentWidth()
                .background(
                    color = if (isSender) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(6.dp)
        ) {


            //Sender name in group
            if (isGroup && !isSender) {
                Text(
                    text = message.user.name!!,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            //Message media
            if (message.media != null) {
                when (message.type) {
                    "image" -> {
                        AsyncImage(
                            model = message.media,
                            contentDescription = "Image",
                            modifier = Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    "video" -> {
                        // Replace with your preferred video player implementation
                        Text("Video content not supported in this preview")
                    }
                    "audio" -> {
                        // Replace with your preferred audio player implementation
                        Text("Audio content not supported in this preview")
                    }
                }
            }

            //Message text
            Column(
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.End,
                modifier = Modifier.widthIn(max = 250.dp).wrapContentWidth()
            ) {
                Text(
                    text = message.message ,
                    modifier = Modifier.padding(start = 8.dp, end = if (message.message.length > 30) 0.dp else 30.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSender) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondary,
                )

                //Time and status
                Row(
                    modifier = Modifier
                        .padding(start = 8.dp, top = if(message.message.length > 30) 4.dp else 0.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = LocalDateTime.parse( message.date, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")).format(DateTimeFormatter.ofPattern("HH:mm")).toString(),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = Color.Gray,
                        textAlign = TextAlign.Start
                    )

                    if (isSender) {
                        Box(
                            contentAlignment = Alignment.BottomCenter,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(start = 4.dp)
                        ) {
                            when (message.status) {
                                "sent" -> Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(14.sp.value.dp)
                                )
                                "received" -> Icon(
                                    Icons.Default.DoneAll,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(14.sp.value.dp)
                                )
                                "read" -> Icon(
                                    Icons.Default.DoneAll,
                                    contentDescription = null,
                                    tint = Color(0xff53bdeb),
                                    modifier = Modifier.size(14.sp.value.dp)
                                )
                                else -> Icon(
                                    Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(14.sp.value.dp)
                                )
                            }
                        }
                    }
                }
            }

        }
    }
}

