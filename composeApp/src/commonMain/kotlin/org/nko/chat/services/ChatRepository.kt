package org.nko.chat.services

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.Filter
import dev.gitlive.firebase.firestore.Query
import dev.gitlive.firebase.firestore.where
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.nko.chat.models.Chat
import org.nko.chat.models.Message
import org.nko.chat.models.User
import kotlinx.datetime.Clock

class ChatRepository(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val firestore = FirebaseManager.firestore
    private val chatQueries = DatabaseManager.db.chatDatabaseQueries

    // Local Data Access
    val localChatsFlow: Flow<List<Chat>> = chatQueries.selectAllChats().asFlow().mapToList(dispatcher).map { entities ->
        entities.map { entity ->
            Chat(
                id = entity.id,
                name = entity.name,
                avatar = entity.avatar,
                isGroup = entity.isGroup ?: false,
                lastMessageAt = entity.lastMessageAt
            )
        }
    }

    fun getLocalMessagesFlow(chatId: String): Flow<List<Message>> =
        chatQueries.selectMessagesForChat(chatId).asFlow().mapToList(dispatcher).map { entities ->
            entities.map { entity ->
                Message(
                    id = entity.id,
                    chatId = entity.chatId,
                    message = entity.text,
                    user = User(id = entity.senderId), // Simplified user, should fetch from Contact table
                    status = entity.status,
                    type = entity.type,
                    media = entity.media,
                    date = entity.createdAt
                )
            }
        }

    // Remote Actions
    suspend fun sendMessage(chatId: String, senderId: String, text: String, type: String = "text") {
        val now = Clock.System.now().toString()
        val messageId = firestore.collection("chats/$chatId/messages").document().id
        
        val message = Message(
            id = messageId,
            chatId = chatId,
            message = text,
            user = User(id = senderId),
            status = "sent",
            type = type,
            media = null,
            date = now
        )

        // Remote push
        firestore.collection("chats/$chatId/messages").document(messageId).set(message)
        
        // Update local database
        chatQueries.insertMessage(
            id = messageId,
            chatId = chatId,
            senderId = senderId,
            text = text,
            status = "sent",
            type = type,
            media = null,
            createdAt = now
        )
        
        chatQueries.insertChat(
            id = chatId,
            name = null, // Or provide name/avatar
            avatar = null,
            isGroup = false,
            lastMessage = text,
            lastMessageAt = now,
            createdBy = null
        )
    }

    suspend fun syncChat(chatId: String) {
        // Here we could listen for changes in Firestore and update the local DB
        firestore.collection("chats/$chatId/messages")
            .orderBy("createdAt", Direction.ASCENDING)
            .snapshots
            .collect { snapshot ->
                snapshot.documents.forEach { doc ->
                    val remoteMessage: Message = doc.data()
                    chatQueries.insertMessage(
                        id = doc.id,
                        chatId = chatId,
                        senderId = remoteMessage.user.id ?: "",
                        text = remoteMessage.message,
                        status = remoteMessage.status,
                        type = remoteMessage.type,
                        media = remoteMessage.media,
                        createdAt = remoteMessage.date
                    )
                }
            }
    }
}
