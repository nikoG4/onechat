package com.onechat.chats.outbox

enum class OutgoingStage { QUEUED, IN_FLIGHT, FAILED, ACKNOWLEDGED }
data class OutgoingItem(
    val conversationId: String,
    val clientMessageId: String,
    val body: String,
    val stage: OutgoingStage = OutgoingStage.QUEUED,
    val attempt: Int = 0,
    val serverId: String? = null,
) {
    init {
        require(conversationId.isNotBlank())
        require(clientMessageId.isNotBlank())
        require(body.isNotBlank())
        require(attempt >= 0)
    }
}
object OutgoingReducer {
    fun enqueue(existing: List<OutgoingItem>, item: OutgoingItem): List<OutgoingItem> {
        require(item.stage == OutgoingStage.QUEUED && item.attempt == 0)
        val same = existing.firstOrNull {
            it.conversationId == item.conversationId && it.clientMessageId == item.clientMessageId
        }
        require(same == null || same.body == item.body) { "A client ID cannot be reused with a different body" }
        return if (same == null) existing + item else existing
    }
    fun start(item: OutgoingItem): OutgoingItem = when (item.stage) {
        OutgoingStage.QUEUED, OutgoingStage.FAILED -> item.copy(
            stage = OutgoingStage.IN_FLIGHT,
            attempt = if (item.attempt == Int.MAX_VALUE) Int.MAX_VALUE else item.attempt + 1,
        )
        OutgoingStage.IN_FLIGHT, OutgoingStage.ACKNOWLEDGED -> item
    }
    fun failed(item: OutgoingItem): OutgoingItem =
        if (item.stage == OutgoingStage.IN_FLIGHT) item.copy(stage = OutgoingStage.FAILED) else item
    fun acknowledged(item: OutgoingItem, serverId: String): OutgoingItem {
        require(serverId.isNotBlank())
        require(item.serverId == null || item.serverId == serverId) { "Conflicting server id" }
        return item.copy(stage = OutgoingStage.ACKNOWLEDGED, serverId = serverId)
    }
}
