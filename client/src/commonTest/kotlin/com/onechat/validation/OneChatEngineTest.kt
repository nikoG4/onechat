package com.onechat.validation

import com.onechat.chats.engine.ChatEngine
import com.onechat.chats.engine.ChatEngineState
import com.onechat.chats.outbox.OutgoingStage
import com.onechat.chats.timeline.DeliveryState
import com.onechat.chats.timeline.TimelineMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class OneChatEngineTest {
    @Test fun offlineQueueSurvivesConnectAndAcknowledgesSameId() {
        var s = ChatEngine.select(ChatEngineState(), "chat-1")
        s = ChatEngine.enqueue(s, "local-1", " hola ", 100L)
        assertEquals(OutgoingStage.QUEUED, s.outbox.single().stage)
        assertEquals("hola", s.outbox.single().body)
        s = ChatEngine.dispatch(s, "chat-1", "local-1")
        assertEquals(OutgoingStage.QUEUED, s.outbox.single().stage)
        s = ChatEngine.connected(ChatEngine.connecting(s))
        s = ChatEngine.dispatch(s, "chat-1", "local-1")
        assertEquals(OutgoingStage.IN_FLIGHT, s.outbox.single().stage)
        s = ChatEngine.acknowledge(s, "chat-1", "local-1", "server-7", 101L)
        assertEquals(OutgoingStage.ACKNOWLEDGED, s.outbox.single().stage)
        assertEquals(1, s.timeline.size)
        assertEquals("server-7", s.timeline.single().serverId)
    }

    @Test fun websocketBeforeHttpAckDoesNotDuplicateAndCannotDowngrade() {
        var s = ChatEngine.connected(ChatEngine.select(ChatEngineState(), "a"))
        s = ChatEngine.enqueue(s, "client-a", "message", 1L)
        s = ChatEngine.incoming(s, TimelineMessage("a", "srv-a", null, "message", 2L, DeliveryState.READ))
        assertEquals(2, s.timeline.size)
        s = ChatEngine.acknowledge(s, "a", "client-a", "srv-a", 3L)
        assertEquals(1, s.timeline.size)
        assertEquals(DeliveryState.READ, s.timeline.single().delivery)
    }

    @Test fun disconnectRetainsOutgoingForIdempotentRetry() {
        var s = ChatEngine.connected(ChatEngine.select(ChatEngineState(), "a"))
        s = ChatEngine.enqueue(s, "cid", "ok", 10L)
        s = ChatEngine.dispatch(s, "a", "cid")
        s = ChatEngine.disconnected(s)
        assertEquals(OutgoingStage.FAILED, s.outbox.single().stage)
        assertTrue(s.realtime.historyRefreshRequired)
        s = ChatEngine.connected(ChatEngine.connecting(s))
        s = ChatEngine.dispatch(s, "a", "cid")
        assertEquals(2, s.outbox.single().attempt)
        assertEquals("cid", s.outbox.single().clientMessageId)
        val ticket = assertNotNull(ChatEngine.beginHistory(s))
        s = ChatEngine.historySucceeded(s, ticket, emptyList())
        assertFalse(s.realtime.historyRefreshRequired)
    }

    @Test fun previousConversationHistoryIgnoredAfterSelection() {
        var s = ChatEngine.connected(ChatEngine.select(ChatEngineState(), "one"))
        val old = assertNotNull(ChatEngine.beginHistory(s))
        s = ChatEngine.select(s, "two")
        val wrong = TimelineMessage("one", serverId = "server1", body = "old",
            createdAtMillis = 5L, delivery = DeliveryState.SENT)
        assertEquals(s, ChatEngine.historySucceeded(s, old, listOf(wrong)))
        val current = assertNotNull(ChatEngine.beginHistory(s))
        val currentMessage = wrong.copy(conversationId = "two")
        s = ChatEngine.historySucceeded(s, current, listOf(currentMessage))
        assertFalse(s.requiresHistoryForSelection)
        assertEquals("two", s.timeline.single().conversationId)
    }

    @Test fun oldHistoryAfterReconnectIgnoredButFreshHistoryAccepted() {
        var s = ChatEngine.connected(ChatEngine.select(ChatEngineState(), "chat"))
        val old = assertNotNull(ChatEngine.beginHistory(s))
        s = ChatEngine.disconnected(s)
        s = ChatEngine.connected(ChatEngine.connecting(s))
        val a = TimelineMessage("chat", serverId = "a", body = "missed", createdAtMillis = 10L, delivery = DeliveryState.SENT)
        s = ChatEngine.historySucceeded(s, old, listOf(a))
        assertTrue(s.timeline.isEmpty())
        val now = assertNotNull(ChatEngine.beginHistory(s))
        s = ChatEngine.historySucceeded(s, now, listOf(a))
        assertEquals(1, s.timeline.size)
        assertFalse(s.realtime.historyRefreshRequired)
    }

    @Test fun conversationMismatchIsRejected() {
        val s = ChatEngine.connected(ChatEngine.select(ChatEngineState(), "right"))
        val ticket = assertNotNull(ChatEngine.beginHistory(s))
        val foreign = TimelineMessage("wrong", serverId = "m", body = "secret", createdAtMillis = 1L, delivery = DeliveryState.SENT)
        assertFailsWith<IllegalArgumentException> { ChatEngine.historySucceeded(s, ticket, listOf(foreign)) }
    }

    @Test fun stableSelectionDoesNotReopenHistory() {
        var s = ChatEngine.connected(ChatEngine.select(ChatEngineState(), "chat"))
        val ticket = assertNotNull(ChatEngine.beginHistory(s))
        s = ChatEngine.historySucceeded(s, ticket, emptyList())
        assertNull(ChatEngine.beginHistory(s))
        assertEquals(s, ChatEngine.select(s, "chat"))
    }
}
