package com.onechat.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random

const val DEFAULT_API_URL = "https://onechat-rl58.onrender.com"

@Serializable data class ApiUser(val id: String, val username: String)
@Serializable data class ApiAuth(val user: ApiUser, val token: String)
@Serializable data class AuthInput(val username: String, val password: String, val inviteCode: String? = null)
@Serializable data class ApiConversation(
    val id: String, val peer: ApiUser, val createdAt: Long, val preview: String? = null
)
@Serializable data class DirectInput(val username: String)
@Serializable data class MessageInput(val clientMessageId: String, val body: String)
@Serializable data class ApiMessage(
    val id: String, val conversationId: String, val senderId: String,
    val clientMessageId: String, val body: String, val createdAt: Long
)
@Serializable data class WsTicket(val ticket: String, val expiresInSeconds: Int)
@Serializable data class MessageEvent(val type: String, val conversationId: String, val message: ApiMessage)
@Serializable data class ApiFailure(val error: String)

class OneChatHttpException(val code: Int, message: String) : Exception(message)

/**
 * Ktor client transport for Android, Desktop and wasmJs.
 * Credentials are deliberately never persisted; re-login is required after restart.
 */
class OneChatApi(
    val baseUrl: String = DEFAULT_API_URL,
    private val client: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; encodeDefaults = true })
        }
        install(WebSockets)
        install(HttpTimeout) {
            requestTimeoutMillis = 25_000
            connectTimeoutMillis = 15_000
        }
        expectSuccess = false
    },
) : AutoCloseable {
    init {
        require(baseUrl.startsWith("https://") && baseUrl.substringAfter("https://").isNotBlank()) {
            "OneChat uses HTTPS only"
        }
    }
    private val root = baseUrl.trimEnd('/')
    private val codec = Json { ignoreUnknownKeys = true }

    private suspend inline fun <reified T> read(response: io.ktor.client.statement.HttpResponse): T {
        if (!response.status.isSuccess()) {
            val reason = try { response.body<ApiFailure>().error } catch (_: Exception) {
                "El servidor respondió HTTP " + response.status.value
            }
            throw OneChatHttpException(response.status.value, reason)
        }
        return response.body()
    }

    suspend fun health(): Boolean = client.get(root + "/ready").status.isSuccess()

    suspend fun register(username: String, password: String, invite: String): ApiAuth =
        read(client.post(root + "/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(AuthInput(username, password, invite))
        })

    suspend fun login(username: String, password: String): ApiAuth =
        read(client.post(root + "/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(AuthInput(username, password))
        })

    suspend fun me(token: String): ApiUser = read(client.get(root + "/v1/me") {
        bearerAuth(token)
    })

    suspend fun search(token: String, query: String): List<ApiUser> =
        read(client.get(root + "/v1/users") {
            bearerAuth(token)
            parameter("q", query)
        })

    suspend fun conversations(token: String): List<ApiConversation> =
        read(client.get(root + "/v1/conversations") { bearerAuth(token) })

    suspend fun direct(token: String, username: String): ApiConversation =
        read(client.post(root + "/v1/conversations/direct") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(DirectInput(username))
        })

    suspend fun history(token: String, conversationId: String, limit: Int = 100): List<ApiMessage> =
        read(client.get(root + "/v1/conversations/" + checkedId(conversationId) + "/messages") {
            bearerAuth(token)
            parameter("limit", limit.coerceIn(1,100))
        })

    suspend fun send(token: String, conversationId: String, input: MessageInput): ApiMessage =
        read(client.post(root + "/v1/conversations/" + checkedId(conversationId) + "/messages") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(input)
        })

    suspend fun wsTicket(token: String): WsTicket =
        read(client.post(root + "/v1/ws-ticket") { bearerAuth(token) })

    suspend fun events(token: String, onConnected: suspend () -> Unit, onMessage: suspend (MessageEvent) -> Unit) {
        val ticket = wsTicket(token).ticket
        val wsUrl = "wss://" + root.removePrefix("https://") + "/v1/ws?ticket=" + ticket
        client.webSocket(urlString = wsUrl) {
            onConnected()
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val message = codec.decodeFromString<MessageEvent>(frame.readText())
                    if (message.type == "message.created") onMessage(message)
                }
            }
        }
    }

    override fun close() = client.close()
}

private val uuidPattern = Regex("[a-fA-F0-9-]{36}")
private fun checkedId(value: String): String {
    require(uuidPattern.matches(value)) { "Identificador de conversación inválido" }
    return value
}

/** Correlation ID, not a credential. Generate once and reuse after uncertain network failure. */
fun newClientMessageId(): String {
    val alphabet = "0123456789abcdef"
    return "client-" + buildString {
        repeat(24) { append(alphabet[Random.nextInt(alphabet.length)]) }
    }
}
