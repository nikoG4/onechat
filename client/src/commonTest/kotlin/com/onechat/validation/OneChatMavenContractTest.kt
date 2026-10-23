package com.onechat.validation

import androidx.compose.ui.unit.dp
import com.onechat.chats.outbox.*
import com.onechat.chats.realtime.*
import com.onechat.chats.timeline.*
import io.github.adaptivekt.core.AdaptiveBreakpoint
import io.github.adaptivekt.core.breakpointForWidth
import kotlin.test.*

class OneChatMavenContractTest {
    @Test
    fun mavenPublishedBreakpointApiAndBoundaries() {
        assertEquals(AdaptiveBreakpoint.Compact, breakpointForWidth(599.dp))
        assertEquals(AdaptiveBreakpoint.Medium, breakpointForWidth(600.dp))
        assertEquals(AdaptiveBreakpoint.Expanded, breakpointForWidth(840.dp))
        assertEquals(AdaptiveBreakpoint.Large, breakpointForWidth(1200.dp))
    }

    @Test
    fun outboxRetryKeepsSameIdAndAcknowledgedCannotDowngrade() {
        val item = OutgoingItem("chat-1", "client-1", "Hola")
        val queued = OutgoingReducer.enqueue(emptyList(), item)
        assertEquals(1, OutgoingReducer.enqueue(queued, item).size)
        val inFlight = OutgoingReducer.start(item)
        val retry = OutgoingReducer.start(OutgoingReducer.failed(inFlight))
        assertEquals("client-1", retry.clientMessageId)
        assertEquals(2, retry.attempt)
        val accepted = OutgoingReducer.acknowledged(retry, "server-1")
        assertEquals(OutgoingStage.ACKNOWLEDGED, OutgoingReducer.failed(accepted).stage)
        assertEquals(2, OutgoingReducer.start(accepted).attempt)
        assertFailsWith<IllegalArgumentException> {
            OutgoingReducer.enqueue(queued, item.copy(body = "mutated"))
        }
    }

    @Test
    fun oldHistoryResponseCannotEraseNewDisconnection() {
        var s = RealtimeRecoveryReducer.connected(RealtimeRecoveryReducer.connecting(RealtimeRecovery()))
        s = RealtimeRecoveryReducer.disconnected(s)
        s = RealtimeRecoveryReducer.connected(RealtimeRecoveryReducer.connecting(s))
        val old = RealtimeRecoveryReducer.beginHistoryRefresh(s)!!
        s = RealtimeRecoveryReducer.disconnected(s)
        s = RealtimeRecoveryReducer.historyRefreshSucceeded(s, old)
        assertTrue(s.historyRefreshRequired)
        s = RealtimeRecoveryReducer.connected(RealtimeRecoveryReducer.connecting(s))
        s = RealtimeRecoveryReducer.historyRefreshSucceeded(s, old)
        assertTrue(s.historyRefreshRequired)
        val current = RealtimeRecoveryReducer.beginHistoryRefresh(s)!!
        s = RealtimeRecoveryReducer.historyRefreshSucceeded(s, current)
        assertFalse(s.historyRefreshRequired)
        assertEquals(30_000L, ReconnectBackoff.delayMillis(30))
    }

    @Test
    fun ackWebsocketEchoAndLateEventsAreIdempotent() {
        val pending = TimelineMessage("chat-1", clientMessageId = "client-1", body = "Hola", createdAtMillis = 1L, delivery = DeliveryState.SENDING)
        val ack = pending.copy(serverId = "server-1", delivery = DeliveryState.SENT)
        var messages = TimelineReconciler.merge(emptyList(), pending)
        messages = TimelineReconciler.merge(messages, ack)
        messages = TimelineReconciler.merge(messages, ack.copy(clientMessageId = null, delivery = DeliveryState.READ))
        messages = TimelineReconciler.merge(messages, ack)
        assertEquals(1, messages.size)
        assertEquals(DeliveryState.READ, messages.single().delivery)
        val otherChat = ack.copy(conversationId = "chat-2")
        assertEquals(2, TimelineReconciler.merge(messages, otherChat).size)
    }

    @Test
    fun bridgeAckRemovesBothTemporaryAndServerAliases() {
        val current = listOf(
            TimelineMessage("chat", clientMessageId = "c", body = "X", createdAtMillis = 1, delivery = DeliveryState.SENDING),
            TimelineMessage("chat", serverId = "s", body = "X", createdAtMillis = 2, delivery = DeliveryState.DELIVERED),
        )
        val bridge = TimelineMessage("chat", serverId = "s", clientMessageId = "c", body = "X", createdAtMillis = 2, delivery = DeliveryState.SENT)
        val merged = TimelineReconciler.merge(current, bridge)
        assertEquals(1, merged.size)
        assertEquals(DeliveryState.DELIVERED, merged.single().delivery)
    }
}
