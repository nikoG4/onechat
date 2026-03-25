package org.nko.chat.services

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.nko.chat.models.response.ChatWsEvent
import kotlinx.coroutines.sync.Mutex

class ChatWebSocketService : BaseService() {

    private val _events = MutableSharedFlow<ChatWsEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<ChatWsEvent> = _events.asSharedFlow()

    private val client = HttpClient(CIO) {
        install(WebSockets)
    }

    @Volatile
    private var session: DefaultClientWebSocketSession? = null

    private var ready = CompletableDeferred<Unit>()
    private val sendMutex = Mutex()

    suspend fun connect() {
        ready = CompletableDeferred()

        val wsUrl = toWsUrl(requireNotNull(serverHost))
        withContext(Dispatchers.Default) {
            client.webSocket(wsUrl.toString()) {
                session = this
                ready.complete(Unit)

                try {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            val event = json.decodeFromString(ChatWsEvent.serializer(), text)
                            _events.emit(event)
                        }
                    }
                } finally {
                    session = null
                }
            }
        }
    }

    suspend fun subscribe(chatId: Int) {
        ready.await()
        val s = session ?: return

        val payload = """{"type":"subscribe","chatId":$chatId}"""
        sendMutex.lock()
        try {
            s.send(Frame.Text(payload))
        } finally {
            sendMutex.unlock()
        }
    }

    fun close() {
        session = null
        client.close()
    }

    private fun toWsUrl(httpUrl: String): String {
        val base = httpUrl.trimEnd('/')
        val wsBase = when {
            base.startsWith("https://") -> base.replace("https://", "wss://")
            base.startsWith("http://") -> base.replace("http://", "ws://")
            else -> base
        }
        return "$wsBase/ws"
    }
}

