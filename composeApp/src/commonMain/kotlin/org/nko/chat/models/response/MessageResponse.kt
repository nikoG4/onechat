package org.nko.chat.models.response

import org.nko.chat.models.Chat
import org.nko.chat.models.User
import kotlinx.serialization.Serializable

@Serializable
data class MessageResponse (
    val id: Int? = null,
    val message: String? = null,
    val type: String? = null,
    val media: String? = null,
    val sender: User? = null,
    val chat: Chat? = null
)
