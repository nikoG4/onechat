package org.nko.chat.models


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Chat(
    val id: String?,
    val name: String? = null,
    val avatar: String? = null,
    @SerialName("group")val isGroup: Boolean = false,
    var messages: List<Message>? = emptyList(),
    val messagePendingCount: Int? = null,
    val participants: List<User>? = null,
    val createdBy: User? = null,
    val createdAt: String? = null
)