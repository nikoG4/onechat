package com.onechat.chats.engine

import com.onechat.chats.outbox.OutgoingItem
import com.onechat.chats.outbox.OutgoingReducer
import com.onechat.chats.outbox.OutgoingStage
import com.onechat.chats.realtime.HistoryRefreshTicket
import com.onechat.chats.realtime.RealtimeRecovery
import com.onechat.chats.realtime.RealtimeRecoveryReducer
import com.onechat.chats.realtime.RealtimeStatus
import com.onechat.chats.timeline.DeliveryState
import com.onechat.chats.timeline.TimelineMessage
import com.onechat.chats.timeline.TimelineReconciler

/** Pure commonMain state. No credentials, HTTP requests or platform-specific storage. */
data class ChatEngineState(
    val selectedConversationId: String? = null,
    val selectionEpoch: Long = 0,
    val requiresHistoryForSelection: Boolean = false,
    val timeline: List<TimelineMessage> = emptyList(),
    val outbox: List<OutgoingItem> = emptyList(),
    val realtime: RealtimeRecovery = RealtimeRecovery(),
)

data class ScopedHistoryTicket(
    val conversationId: String,
    val selectionEpoch: Long,
    val gapGeneration: Long,
    val recoveryTicket: HistoryRefreshTicket?,
)

/** Every call returns new state; a caller owns persistence and the Ktor / browser transport. */
object ChatEngine {
    fun select(state: ChatEngineState, conversationId: String?): ChatEngineState {
        if (state.selectedConversationId == conversationId) return state
        return state.copy(
            selectedConversationId = conversationId,
            selectionEpoch = state.selectionEpoch + 1,
            requiresHistoryForSelection = conversationId != null,
        )
    }

    /** Caller generates a stable unique clientMessageId (and persists it before network IO). */
    fun enqueue(state: ChatEngineState, clientMessageId: String, body: String, timestamp: Long): ChatEngineState {
        val id = requireNotNull(state.selectedConversationId) { "Select a conversation before sending" }
        val text = body.trim()
        require(text.isNotEmpty()) { "Cannot send a blank message" }
        val draft = OutgoingItem(id, clientMessageId, text)
        val queue = OutgoingReducer.enqueue(state.outbox, draft)
        val optimistic = TimelineMessage(
            conversationId = id, clientMessageId = clientMessageId,
            body = text, createdAtMillis = timestamp, delivery = DeliveryState.QUEUED,
        )
        return state.copy(
            outbox = queue,
            timeline = TimelineReconciler.merge(state.timeline, optimistic),
        )
    }

    fun dispatch(state: ChatEngineState, conversationId: String, clientMessageId: String): ChatEngineState {
        if (state.realtime.status != RealtimeStatus.CONNECTED) return state
        val current = state.outbox.find { it.conversationId == conversationId && it.clientMessageId == clientMessageId }
            ?: return state
        val next = OutgoingReducer.start(current)
        return state.copy(
            outbox = state.outbox.map { if (it === current) next else it },
            timeline = if (next.stage == OutgoingStage.IN_FLIGHT) TimelineReconciler.merge(
                state.timeline,
                TimelineMessage(conversationId, clientMessageId = clientMessageId, body = current.body,
                    createdAtMillis = 0, delivery = DeliveryState.SENDING),
            ) else state.timeline,
        )
    }

    fun failed(state: ChatEngineState, conversationId: String, clientMessageId: String): ChatEngineState {
        val current = state.outbox.find { it.conversationId == conversationId && it.clientMessageId == clientMessageId }
            ?: return state
        if (current.stage != OutgoingStage.IN_FLIGHT) return state
        return state.copy(
            outbox = state.outbox.map {
                if (it === current) OutgoingReducer.failed(it) else it
            },
            timeline = TimelineReconciler.merge(state.timeline, TimelineMessage(
                conversationId, clientMessageId = clientMessageId,
                body = current.body, createdAtMillis = 0, delivery = DeliveryState.FAILED,
            )),
        )
    }

    /** An authenticated HTTP acknowledgement bridges optimistic/client and server IDs. */
    fun acknowledge(
        state: ChatEngineState,
        conversationId: String,
        clientMessageId: String,
        serverId: String,
        timestamp: Long,
    ): ChatEngineState {
        val current = state.outbox.find { it.conversationId == conversationId && it.clientMessageId == clientMessageId }
            ?: return state
        val next = OutgoingReducer.acknowledged(current, serverId)
        val delivered = TimelineMessage(
            conversationId, serverId, clientMessageId,
            body = current.body, createdAtMillis = timestamp, delivery = DeliveryState.SENT,
        )
        return state.copy(
            outbox = state.outbox.map { if (it === current) next else it },
            timeline = TimelineReconciler.merge(state.timeline, delivered),
        )
    }

    /** An authenticated WebSocket event or GET snapshot; events may arrive in any order. */
    fun incoming(state: ChatEngineState, message: TimelineMessage): ChatEngineState =
        state.copy(timeline = TimelineReconciler.merge(state.timeline, message))

    fun disconnected(state: ChatEngineState): ChatEngineState =
        state.copy(
            realtime = RealtimeRecoveryReducer.disconnected(state.realtime),
            requiresHistoryForSelection = state.selectedConversationId != null,
            outbox = state.outbox.map {
                if (it.stage == OutgoingStage.IN_FLIGHT) OutgoingReducer.failed(it) else it
            },
        )

    fun connecting(state: ChatEngineState): ChatEngineState =
        state.copy(realtime = RealtimeRecoveryReducer.connecting(state.realtime))

    fun connected(state: ChatEngineState): ChatEngineState =
        state.copy(realtime = RealtimeRecoveryReducer.connected(state.realtime))

    fun beginHistory(state: ChatEngineState): ScopedHistoryTicket? {
        val id = state.selectedConversationId ?: return null
        if (state.realtime.status != RealtimeStatus.CONNECTED) return null
        val recovery = RealtimeRecoveryReducer.beginHistoryRefresh(state.realtime)
        if (!state.requiresHistoryForSelection && recovery == null) return null
        return ScopedHistoryTicket(id, state.selectionEpoch, state.realtime.gapGeneration, recovery)
    }

    /** Discard stale HTTP response if selection changed or a new WebSocket gap opened. */
    fun historySucceeded(
        state: ChatEngineState, ticket: ScopedHistoryTicket, page: List<TimelineMessage>,
    ): ChatEngineState {
        if (ticket.conversationId != state.selectedConversationId ||
            ticket.selectionEpoch != state.selectionEpoch ||
            ticket.gapGeneration != state.realtime.gapGeneration ||
            state.realtime.status != RealtimeStatus.CONNECTED
        ) return state
        if (ticket.recoveryTicket != null &&
            ticket.recoveryTicket.gapGeneration != state.realtime.gapGeneration
        ) return state
        require(page.all { it.conversationId == ticket.conversationId }) { "Cross-conversation history rejected" }
        return state.copy(
            timeline = TimelineReconciler.mergeAll(state.timeline, page),
            requiresHistoryForSelection = false,
            realtime = ticket.recoveryTicket?.let {
                RealtimeRecoveryReducer.historyRefreshSucceeded(state.realtime, it)
            } ?: state.realtime,
        )
    }
}
