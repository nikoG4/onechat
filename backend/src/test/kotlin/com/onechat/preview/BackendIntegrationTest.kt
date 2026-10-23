package com.onechat.preview

import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import org.junit.Test
import java.util.UUID
import kotlin.test.*

class BackendIntegrationTest {
    private val cfg = PreviewConfig(
        jdbcUrl = System.getenv("ONECHAT_TEST_DB_URL")
            ?: error("CI must provide ONECHAT_TEST_DB_URL (real PostgreSQL required)"),
        dbUser = System.getenv("ONECHAT_TEST_DB_USER") ?: "onechat",
        dbPassword = System.getenv("ONECHAT_TEST_DB_PASSWORD") ?: "test_only",
        jwtSecret = "preview-test-secret-0123456789-".repeat(3),
        inviteCode = "local-test-invite-secret",
    )

    private fun createUsername(prefix: String): String =
        prefix + UUID.randomUUID().toString().replace("-", "").take(10)

    @Test fun authenticationMembershipPersistenceAndIdempotence() {
        ChatDatabase(cfg).use { db ->
            testApplication {
                application { installOneChatPreview(cfg, db) }
                val client = createClient {
                    install(ContentNegotiation) { json() }
                }
                suspend fun register(name: String): AuthReply {
                    val response = client.post("/v1/auth/register") {
                        contentType(ContentType.Application.Json)
                        setBody(Credentials(name, "correct-password-123", cfg.inviteCode))
                    }
                    assertEquals(HttpStatusCode.Created, response.status)
                    return response.body()
                }
                suspend fun authorizedGet(path: String, token: String) = client.get(path) {
                    header(HttpHeaders.Authorization, "Bearer " + token)
                }
                val aliceName = createUsername("alice")
                val bobName = createUsername("bob")
                val charlieName = createUsername("charlie")
                assertEquals(HttpStatusCode.OK, client.get("/health").status)
                assertEquals(HttpStatusCode.OK, client.get("/ready").status)
                assertEquals(HttpStatusCode.Unauthorized, client.get("/v1/me").status)
                val denied = client.post("/v1/auth/register") {
                    contentType(ContentType.Application.Json)
                    setBody(Credentials(aliceName, "correct-password-123", "incorrect-invite"))
                }
                assertEquals(HttpStatusCode.Forbidden, denied.status)
                val alice = register(aliceName)
                val bob = register(bobName)
                val charlie = register(charlieName)
                val dup = client.post("/v1/auth/register") {
                    contentType(ContentType.Application.Json)
                    setBody(Credentials(aliceName.uppercase(), "correct-password-123", cfg.inviteCode))
                }
                assertEquals(HttpStatusCode.Conflict, dup.status)
                val wrongPass = client.post("/v1/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody(Credentials(aliceName, "incorrect-password"))
                }
                assertEquals(HttpStatusCode.Unauthorized, wrongPass.status)
                val login = client.post("/v1/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody(Credentials(aliceName.uppercase(), "correct-password-123"))
                }
                assertEquals(HttpStatusCode.OK, login.status)
                assertEquals(alice.user.id, login.body<AuthReply>().user.id)
                assertEquals(alice.user.id, authorizedGet("/v1/me", alice.token).body<UserDto>().id)
                assertEquals(HttpStatusCode.Unauthorized,
                    authorizedGet("/v1/me", "not-a-jwt").status)

                val users = authorizedGet("/v1/users?q=bob", alice.token).body<List<UserDto>>()
                assertTrue(users.any { it.id == bob.user.id })
                val direct = client.post("/v1/conversations/direct") {
                    header(HttpHeaders.Authorization, "Bearer " + alice.token)
                    contentType(ContentType.Application.Json)
                    setBody(DirectRequest(bobName))
                }
                assertEquals(HttpStatusCode.OK, direct.status)
                val conversation = direct.body<ConversationDto>()
                assertEquals(bob.user.id, conversation.peer.id)
                val same = client.post("/v1/conversations/direct") {
                    header(HttpHeaders.Authorization, "Bearer " + bob.token)
                    contentType(ContentType.Application.Json)
                    setBody(DirectRequest(aliceName))
                }.body<ConversationDto>()
                assertEquals(conversation.id, same.id)

                assertEquals(HttpStatusCode.Forbidden,
                    authorizedGet("/v1/conversations/" + conversation.id + "/messages", charlie.token).status)

                suspend fun send(content: String, cid: String = "client-abc-001") = client.post(
                    "/v1/conversations/" + conversation.id + "/messages"
                ) {
                    header(HttpHeaders.Authorization, "Bearer " + alice.token)
                    contentType(ContentType.Application.Json)
                    setBody(MessageRequest(cid, content))
                }
                val first = send("Hello from real PostgreSQL")
                assertEquals(HttpStatusCode.Created, first.status)
                val posted = first.body<MessageDto>()
                assertEquals(alice.user.id, posted.senderId)
                val again = send("Hello from real PostgreSQL")
                assertEquals(HttpStatusCode.OK, again.status)
                assertEquals(posted.id, again.body<MessageDto>().id)
                assertEquals(HttpStatusCode.Conflict, send("Changed text").status)
                assertEquals(HttpStatusCode.BadRequest, send("  ").status)
                val unauthorizedSend = client.post(
                    "/v1/conversations/" + conversation.id + "/messages"
                ) {
                    header(HttpHeaders.Authorization, "Bearer " + charlie.token)
                    contentType(ContentType.Application.Json)
                    setBody(MessageRequest("client-abc-777", "No access"))
                }
                assertEquals(HttpStatusCode.Forbidden, unauthorizedSend.status)
                val history = authorizedGet(
                    "/v1/conversations/" + conversation.id + "/messages", bob.token
                ).body<List<MessageDto>>()
                assertEquals(1, history.size)
                assertEquals(posted.id, history.single().id)
                assertNull(history.single().readAt)
                val bobUnread = authorizedGet("/v1/conversations", bob.token)
                    .body<List<ConversationDto>>().first { it.id == conversation.id }.unread
                assertEquals(1, bobUnread)
                assertEquals(0, authorizedGet("/v1/conversations", alice.token)
                    .body<List<ConversationDto>>().first { it.id == conversation.id }.unread)
                assertEquals(HttpStatusCode.Forbidden,
                    client.post("/v1/conversations/" + conversation.id + "/read") {
                        header(HttpHeaders.Authorization, "Bearer " + charlie.token)
                    }.status)
                val receiptResponse = client.post("/v1/conversations/" + conversation.id + "/read") {
                    header(HttpHeaders.Authorization, "Bearer " + bob.token)
                }
                assertEquals(HttpStatusCode.OK, receiptResponse.status)
                val receipt = receiptResponse.body<ReadReceiptDto>()
                assertEquals(bob.user.id, receipt.readerId)
                assertNotNull(authorizedGet("/v1/conversations/" + conversation.id + "/messages",
                    alice.token).body<List<MessageDto>>().single().readAt)
                assertEquals(0, authorizedGet("/v1/conversations", bob.token)
                    .body<List<ConversationDto>>().first { it.id == conversation.id }.unread)
                assertEquals(HttpStatusCode.OK,
                    client.post("/v1/conversations/" + conversation.id + "/read") {
                        header(HttpHeaders.Authorization, "Bearer " + bob.token)
                    }.status)
                assertEquals(0, authorizedGet(
                    "/v1/conversations/" + conversation.id + "/messages?beforeId=" + posted.id,
                    bob.token
                ).body<List<MessageDto>>().size)
                assertEquals(HttpStatusCode.BadRequest, authorizedGet(
                    "/v1/conversations/" + conversation.id + "/messages?limit=999",
                    alice.token
                ).status)

                // Re-open a new DB pool to verify persistence across app sessions.
                ChatDatabase(cfg).use { secondConnection ->
                    assertEquals(posted.id, secondConnection.history(conversation.id, 20, null).single().id)
                    assertEquals(2, secondConnection.members(conversation.id).size)
                }
            }
        }
    }

    @Test fun oneTimeTicketAndRealtimeDelivery() {
        ChatDatabase(cfg).use { db ->
            testApplication {
                application { installOneChatPreview(cfg, db) }
                val client = createClient {
                    install(ContentNegotiation) { json() }
                    install(io.ktor.client.plugins.websocket.WebSockets)
                }
                suspend fun register(prefix: String): AuthReply {
                    val response = client.post("/v1/auth/register") {
                        contentType(ContentType.Application.Json)
                        setBody(Credentials(createUsername(prefix), "another-correct-password", cfg.inviteCode))
                    }
                    assertEquals(HttpStatusCode.Created, response.status)
                    return response.body()
                }
                val a = register("socketA")
                val b = register("socketB")
                val chat = client.post("/v1/conversations/direct") {
                    header(HttpHeaders.Authorization, "Bearer " + a.token)
                    contentType(ContentType.Application.Json)
                    setBody(DirectRequest(b.user.username))
                }.body<ConversationDto>()
                val ticket = client.post("/v1/ws-ticket") {
                    header(HttpHeaders.Authorization, "Bearer " + b.token)
                }.body<WsTicketReply>()
                assertEquals(60, ticket.expiresInSeconds)
                client.webSocket(urlString = "/v1/ws?ticket=" + ticket.ticket) {
                    val sent = client.post("/v1/conversations/" + chat.id + "/messages") {
                        header(HttpHeaders.Authorization, "Bearer " + a.token)
                        contentType(ContentType.Application.Json)
                        setBody(MessageRequest("client-socket-123", "Delivered via WebSocket"))
                    }
                    assertEquals(HttpStatusCode.Created, sent.status)
                    val frame = withTimeout(5_000L) { incoming.receive() as Frame.Text }
                    val event = Json.decodeFromString<MessageEvent>(frame.readText())
                    assertEquals("message.created", event.type)
                    assertEquals(chat.id, event.conversationId)
                    assertEquals("Delivered via WebSocket", event.message?.body)
                    val acknowledged = client.post("/v1/conversations/" + chat.id + "/read") {
                        header(HttpHeaders.Authorization, "Bearer " + b.token)
                    }.body<ReadReceiptDto>()
                    assertEquals(b.user.id, acknowledged.readerId)
                    val receiptFrame = withTimeout(5_000L) { incoming.receive() as Frame.Text }
                    val receiptEvent = Json.decodeFromString<MessageEvent>(receiptFrame.readText())
                    assertEquals("conversation.read", receiptEvent.type)
                    assertEquals(b.user.id, receiptEvent.receipt?.readerId)
                }
            }
        }
    }

    @Test fun oneTimeTicketIsNotReusable() {
        val tickets = WsTickets()
        val ticket = tickets.issue("user-1")
        assertEquals("user-1", tickets.consume(ticket))
        assertNull(tickets.consume(ticket))
        assertNull(tickets.consume("incorrect"))
        assertNull(tickets.consume(null))
    }

    @Test fun insecureConfigurationRejected() {
        assertFailsWith<IllegalArgumentException> { cfg.copy(jwtSecret = "too short") }
        assertFailsWith<IllegalArgumentException> { cfg.copy(inviteCode = "short") }
        assertFailsWith<IllegalArgumentException> { cfg.copy(allowedOrigins = listOf("https://host.test/path")) }
    }
}
