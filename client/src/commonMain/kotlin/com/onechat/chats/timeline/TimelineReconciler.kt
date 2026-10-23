package com.onechat.chats.timeline

enum class DeliveryState { QUEUED, SENDING, SENT, DELIVERED, READ, FAILED }
data class TimelineMessage(
    val conversationId: String,
    val serverId: String? = null,
    val clientMessageId: String? = null,
    val body: String,
    val createdAtMillis: Long,
    val delivery: DeliveryState,
) {
    init {
        require(conversationId.isNotBlank())
        require(!serverId.isNullOrBlank() || !clientMessageId.isNullOrBlank())
    }
}
object TimelineReconciler {
    fun merge(current: List<TimelineMessage>, incoming: TimelineMessage): List<TimelineMessage> {
        val matches = current.filter { previous ->
            previous.conversationId == incoming.conversationId && (
                (!previous.serverId.isNullOrBlank() && previous.serverId == incoming.serverId) ||
                (!previous.clientMessageId.isNullOrBlank() && previous.clientMessageId == incoming.clientMessageId)
            )
        }
        var reconciled = incoming
        for (old in matches) {
            reconciled = old.copy(
                serverId = reconciled.serverId ?: old.serverId,
                clientMessageId = reconciled.clientMessageId ?: old.clientMessageId,
                body = if (reconciled.body.isNotEmpty()) reconciled.body else old.body,
                createdAtMillis = if (reconciled.createdAtMillis > 0) reconciled.createdAtMillis else old.createdAtMillis,
                delivery = strongest(old.delivery, reconciled.delivery),
            )
        }
        return (current.filterNot { it in matches } + reconciled).sortedWith(
            compareBy<TimelineMessage> { it.createdAtMillis }
                .thenBy { it.serverId ?: it.clientMessageId ?: "" }
        )
    }
    fun mergeAll(current: List<TimelineMessage>, incoming: Iterable<TimelineMessage>): List<TimelineMessage> =
        incoming.fold(current) { acc, item -> merge(acc, item) }
    fun strongest(old: DeliveryState, new: DeliveryState): DeliveryState {
        if (old == DeliveryState.FAILED && new != DeliveryState.FAILED) return new
        if (new == DeliveryState.FAILED) return if (old == DeliveryState.QUEUED || old == DeliveryState.SENDING) new else old
        val ranks = mapOf(
            DeliveryState.QUEUED to 0, DeliveryState.SENDING to 1,
            DeliveryState.SENT to 2, DeliveryState.DELIVERED to 3,
            DeliveryState.READ to 4, DeliveryState.FAILED to -1,
        )
        return if (ranks.getValue(new) > ranks.getValue(old)) new else old
    }
}
