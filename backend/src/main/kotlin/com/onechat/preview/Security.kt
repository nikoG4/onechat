package com.onechat.preview

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.Date
import java.util.concurrent.ConcurrentHashMap

class PreviewJwt(private val secret: String) {
    private val algo = Algorithm.HMAC256(secret)
    private val verifier: JWTVerifier = JWT.require(algo).withIssuer("onechat-preview").build()
    fun issue(user: UserDto): String = JWT.create()
        .withIssuer("onechat-preview")
        .withSubject(user.id)
        .withIssuedAt(Date())
        .withExpiresAt(Date(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000))
        .sign(algo)

    fun subject(token: String): String? =
        try { verifier.verify(token).subject } catch (_: JWTVerificationException) { null }
}

fun constantTimeEquals(a: String, b: String): Boolean =
    MessageDigest.isEqual(a.toByteArray(Charsets.UTF_8), b.toByteArray(Charsets.UTF_8))

/**
 * One-time, short-lived WS bearer. Never log the ticket query string.
 * In-memory by design: the preview is pinned to ONE service instance.
 */
class WsTickets {
    private val random = SecureRandom()
    private val active = ConcurrentHashMap<String, Pair<String, Long>>()
    private val lifetimeMillis = 60_000L

    @Synchronized fun issue(userId: String): String {
        val now = System.currentTimeMillis()
        active.entries.removeIf { it.value.second <= now }
        check(active.size < 2_000) { "Too many pending websocket handshakes" }
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        val code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        active[code] = userId to (now + lifetimeMillis)
        return code
    }

    @Synchronized fun consume(ticket: String?): String? {
        if (ticket.isNullOrBlank()) return null
        val pair = active.remove(ticket) ?: return null
        return pair.first.takeIf { pair.second > System.currentTimeMillis() }
    }
}

class PreviewRateLimiter {
    private data class Window(val start: Long, val requests: Int)
    private val windows = ConcurrentHashMap<String, Window>()
    @Synchronized fun allow(key: String, max: Int = 15): Boolean {
        val now = System.currentTimeMillis()
        if (windows.size > 10_000) windows.entries.removeIf { now - it.value.start >= 60_000L }
        val old = windows[key]
        val next = if (old == null || now - old.start >= 60_000L) Window(now, 1)
            else Window(old.start, old.requests + 1)
        windows[key] = next
        return next.requests <= max
    }
}
