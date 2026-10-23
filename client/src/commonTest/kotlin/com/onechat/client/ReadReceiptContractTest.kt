package com.onechat.client

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReadReceiptContractTest {
    private val codec = Json { ignoreUnknownKeys = true }

    @Test
    fun oldMessageWithoutReadStateRemainsCompatible() {
        val old = """
          {"id":"msg","conversationId":"chat","senderId":"alice",
           "clientMessageId":"client-old-00001","body":"hola","createdAt":1000}
        """.trimIndent()
        val message = codec.decodeFromString<ApiMessage>(old)
        assertNull(message.readAt)
    }

    @Test
    fun decodeServerReadReceiptAndUnreadCount() {
        val receipt = codec.decodeFromString<MessageEvent>(
            """{"type":"conversation.read","conversationId":"chat",
               "message":null,"receipt":{"conversationId":"chat","readerId":"bob","readAt":1234}}"""
        )
        assertEquals("conversation.read", receipt.type)
        assertEquals("bob", receipt.receipt?.readerId)
        assertNull(receipt.message)
        val chat = codec.decodeFromString<ApiConversation>(
            """{"id":"chat","peer":{"id":"bob","username":"bob"},
                "createdAt":1000,"preview":"hola","unread":2}"""
        )
        assertEquals(2, chat.unread)
    }
}
