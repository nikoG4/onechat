package com.onechat.client

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OneChatApiContractTest {
    @Test
    fun authenticatesAndSendsThroughRealBackendRoutes() = runBlocking {
        val requested = mutableListOf<String>()
        val authHeaders = mutableListOf<String>()
        val mock = MockEngine { request ->
            val url = request.url
            requested += request.method.value + " " + url.encodedPath
            authHeaders += request.headers[HttpHeaders.Authorization].orEmpty()
            val payload = when (url.encodedPath) {
                "/v1/auth/login" ->
                    """{"user":{"id":"user-A","username":"alice"},"token":"test-jwt"}"""
                "/v1/me" -> """{"id":"user-A","username":"alice"}"""
                "/v1/conversations" -> """[{"id":"12345678-1234-1234-1234-123456789abc","peer":{"id":"user-B","username":"bob"},"createdAt":1,"preview":null}]"""
                "/v1/conversations/12345678-1234-1234-1234-123456789abc/messages" ->
                    if (request.method == HttpMethod.Get) "[]" else
                    """{"id":"message-1","conversationId":"12345678-1234-1234-1234-123456789abc","senderId":"user-A","clientMessageId":"client-0123456789","body":"hola","createdAt":1000}"""
                "/v1/ws-ticket" -> """{"ticket":"temporary-ticket","expiresInSeconds":60}"""
                else -> """{"error":"not found"}"""
            }
            respond(payload, status = if (url.encodedPath in setOf(
                "/v1/auth/login", "/v1/me", "/v1/conversations",
                "/v1/conversations/12345678-1234-1234-1234-123456789abc/messages",
                "/v1/ws-ticket",
            )) HttpStatusCode.OK else HttpStatusCode.NotFound,
                headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = HttpClient(mock) {
            install(ContentNegotiation) { json() }
            expectSuccess = false
        }
        val api = OneChatApi(client = client)
        try {
            val auth = api.login("alice", "password")
            assertEquals("test-jwt", auth.token)
            assertEquals("alice", api.me(auth.token).username)
            val chats = api.conversations(auth.token)
            assertEquals("bob", chats.single().peer.username)
            assertTrue(api.history(auth.token, chats.single().id).isEmpty())
            val sent = api.send(auth.token, chats.single().id, MessageInput("client-0123456789", "hola"))
            assertEquals("message-1", sent.id)
            assertEquals("temporary-ticket", api.wsTicket(auth.token).ticket)
            assertEquals(6, requested.size)
            assertEquals("POST /v1/auth/login", requested.first())
            assertEquals("POST /v1/conversations/12345678-1234-1234-1234-123456789abc/messages", requested[4])
            assertTrue(authHeaders.drop(1).all { it == "Bearer test-jwt" })
        } finally {
            api.close()
        }
    }

    @Test
    fun rejectsInsecureOriginAndInvalidConversationId() = runBlocking {
        assertFailsWith<IllegalArgumentException> {
            OneChatApi(baseUrl = "http://insecure.example.org")
        }
        val client = HttpClient(MockEngine { respond("[]", headers = headersOf(HttpHeaders.ContentType, "application/json")) }) {
            install(ContentNegotiation) { json() }
        }
        val api = OneChatApi(client = client)
        try {
            assertFailsWith<IllegalArgumentException> { api.history("token", "../another-user") }
            assertTrue(newClientMessageId().matches(Regex("client-[0-9a-f]{24}")))
        } finally {
            api.close()
        }
    }
}
