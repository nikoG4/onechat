package org.nko.chat.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    var id: String?,
    val message: String,
    var date: String,
    @SerialName("sender")val user: User,
    var status: String,
    val type: String,
    val media: String?,
    val chat: Chat? = null,
    @SerialName("chatId") val chatId: String? = null,
    val recipients: List<User>? = null
)

