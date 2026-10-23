package com.onechat.chats.realtime

enum class RealtimeStatus { OFFLINE, CONNECTING, CONNECTED, RECONNECTING }
data class RealtimeRecovery(
    val status: RealtimeStatus = RealtimeStatus.OFFLINE,
    val reconnectAttempt: Int = 0,
    val historyRefreshRequired: Boolean = false,
    val gapGeneration: Long = 0,
)
data class HistoryRefreshTicket(val gapGeneration: Long)
object RealtimeRecoveryReducer {
    fun connecting(old: RealtimeRecovery): RealtimeRecovery = old.copy(
        status = if (old.historyRefreshRequired) RealtimeStatus.RECONNECTING else RealtimeStatus.CONNECTING,
    )
    fun disconnected(old: RealtimeRecovery): RealtimeRecovery = old.copy(
        status = RealtimeStatus.OFFLINE,
        reconnectAttempt = (old.reconnectAttempt + 1).coerceAtMost(30),
        historyRefreshRequired = true,
        gapGeneration = if (old.gapGeneration == Long.MAX_VALUE) 0 else old.gapGeneration + 1,
    )
    fun connected(old: RealtimeRecovery): RealtimeRecovery = old.copy(
        status = RealtimeStatus.CONNECTED, reconnectAttempt = 0,
    )
    fun beginHistoryRefresh(old: RealtimeRecovery): HistoryRefreshTicket? =
        if (old.status == RealtimeStatus.CONNECTED && old.historyRefreshRequired)
            HistoryRefreshTicket(old.gapGeneration) else null
    fun historyRefreshSucceeded(old: RealtimeRecovery, ticket: HistoryRefreshTicket): RealtimeRecovery =
        if (old.status == RealtimeStatus.CONNECTED && old.gapGeneration == ticket.gapGeneration)
            old.copy(historyRefreshRequired = false) else old
    fun historyRefreshFailed(old: RealtimeRecovery, ticket: HistoryRefreshTicket): RealtimeRecovery =
        if (old.gapGeneration == ticket.gapGeneration)
            old.copy(historyRefreshRequired = true) else old
}
object ReconnectBackoff {
    fun delayMillis(attemptZeroBased: Int): Long {
        var delay = 750L
        repeat(attemptZeroBased.coerceIn(0, 30)) {
            delay = (delay * 2L).coerceAtMost(30_000L)
        }
        return delay
    }
}
