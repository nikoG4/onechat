package com.onechat.client

import androidx.compose.runtime.Composable
import kotlinx.serialization.Serializable

/**
 * Persisted payload deliberately excludes credentials, JWTs, drafts, websocket tickets,
 * private key material and any media. The server still stores plaintext in this preview.
 */
@Serializable
data class LocalChatSnapshot(
    val version: Int = 1,
    val conversations: List<ApiConversation> = emptyList(),
    val messages: List<ApiMessage> = emptyList(),
) {
    init {
        require(version == 1) { "Unsupported vault format" }
    }

    fun bounded(): LocalChatSnapshot = copy(
        conversations = conversations.take(300),
        messages = messages.sortedWith(
            compareBy<ApiMessage> { it.createdAt }.thenBy { it.id }
        ).takeLast(4000),
    )
}

/** Only Android persists; non-Android implementations are deliberately non-persistent. */
interface ChatLocalStore {
    suspend fun restore(userId: String): LocalChatSnapshot
    suspend fun save(userId: String, snapshot: LocalChatSnapshot)
}

/** No filesystem, preferences, IndexedDB, localStorage, or disk writes. */
class EphemeralChatStore : ChatLocalStore {
    override suspend fun restore(userId: String): LocalChatSnapshot = LocalChatSnapshot()
    override suspend fun save(userId: String, snapshot: LocalChatSnapshot) = Unit
}

@Composable
expect fun rememberChatLocalStore(): ChatLocalStore
