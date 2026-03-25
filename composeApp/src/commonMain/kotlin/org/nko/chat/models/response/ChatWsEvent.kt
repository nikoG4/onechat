package org.nko.chat.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.nko.chat.models.Message

@Serializable
data class ChatWsEvent(
    val type: String,
    @SerialName("chatId") val chatId: Int,
    val message: Message
)

