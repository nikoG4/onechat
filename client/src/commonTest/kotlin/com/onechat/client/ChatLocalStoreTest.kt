package com.onechat.client

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class ChatLocalStoreTest {
    private fun msg(index: Int) = ApiMessage(
        id = "m" + index, conversationId = "c", senderId = "sender",
        clientMessageId = "client" + index, body = "body " + index,
        createdAt = index.toLong()
    )

    @Test
    fun desktopAndWebStoreNeverRestoreMessagesOrCredentials() = runBlocking {
        val store = EphemeralChatStore()
        store.save("account", LocalChatSnapshot(messages = listOf(msg(1))))
        assertTrue(store.restore("account").messages.isEmpty())
        assertTrue(store.restore("other").messages.isEmpty())
    }

    @Test
    fun snapshotsBoundAndKeepMostRecentMessages() {
        val snapshot = LocalChatSnapshot(messages = (1..4500).map(::msg)).bounded()
        assertEquals(4000, snapshot.messages.size)
        assertEquals("m501", snapshot.messages.first().id)
        assertEquals("m4500", snapshot.messages.last().id)
    }

    @Test
    fun rejectUnsupportedSnapshotVersion() {
        assertFailsWith<IllegalArgumentException> { LocalChatSnapshot(version = 9) }
    }
}
