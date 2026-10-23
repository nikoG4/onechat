package com.onechat.preview

import kotlinx.serialization.Serializable

@Serializable data class ApiError(val error: String)
@Serializable data class Credentials(val username: String, val password: String, val inviteCode: String? = null)
@Serializable data class UserDto(val id: String, val username: String)
@Serializable data class AuthReply(val user: UserDto, val token: String)
@Serializable data class DirectRequest(val username: String)
@Serializable data class ConversationDto(
    val id: String, val peer: UserDto, val createdAt: Long, val preview: String? = null,
    val unread: Int = 0,
)
@Serializable data class MessageRequest(val clientMessageId: String, val body: String)
@Serializable data class MessageDto(
    val id: String, val conversationId: String, val senderId: String,
    val clientMessageId: String, val body: String, val createdAt: Long,
    val readAt: Long? = null,
)
@Serializable data class WsTicketReply(val ticket: String, val expiresInSeconds: Int)
@Serializable data class ReadReceiptDto(val conversationId: String, val readerId: String, val readAt: Long)
@Serializable data class MessageEvent(
    val type: String, val conversationId: String,
    val message: MessageDto? = null, val receipt: ReadReceiptDto? = null,
)
