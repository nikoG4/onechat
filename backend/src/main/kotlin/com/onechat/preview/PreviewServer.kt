package com.onechat.preview

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.mindrot.jbcrypt.BCrypt
import java.net.URI
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

private val namePattern = Regex("[A-Za-z][A-Za-z0-9_]{2,23}")
private val messageIdPattern = Regex("[A-Za-z0-9_.:-]{8,80}")
private val jsonCodec = Json { ignoreUnknownKeys = false; encodeDefaults = true }

private class RealtimeHub {
    private val clients = ConcurrentHashMap<String, CopyOnWriteArraySet<DefaultWebSocketServerSession>>()
    fun join(user: String, session: DefaultWebSocketServerSession) {
        clients.computeIfAbsent(user) { CopyOnWriteArraySet() }.add(session)
    }
    fun leave(user: String, session: DefaultWebSocketServerSession) {
        clients[user]?.remove(session)
    }
    suspend fun emit(members: List<String>, msg: MessageDto) {
        val payload = jsonCodec.encodeToString(MessageEvent("message.created", msg.conversationId, msg))
        for (member in members.distinct()) {
            for (socket in clients[member] ?: emptySet()) {
                try { socket.send(Frame.Text(payload)) } catch (_: Exception) { leave(member, socket) }
            }
        }
    }
}

private suspend fun ApplicationCall.user(jwt: PreviewJwt, db: ChatDatabase): UserDto? {
    val authorization = request.headers[HttpHeaders.Authorization].orEmpty()
    val token = if (authorization.startsWith("Bearer ", ignoreCase = true))
        authorization.substringAfter(' ').trim() else ""
    val resolved = jwt.subject(token).let { sub -> if (sub == null) null else db.findUser(sub) }
    if (resolved == null) respond(HttpStatusCode.Unauthorized, ApiError("Unauthorized"))
    return resolved
}

fun Application.installOneChatPreview(config: PreviewConfig, db: ChatDatabase) {
    db.migrate()
    val jwt = PreviewJwt(config.jwtSecret)
    val tickets = WsTickets()
    val limiter = PreviewRateLimiter()
    val hub = RealtimeHub()

    install(ContentNegotiation) { json(jsonCodec) }
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(20)
        timeout = Duration.ofSeconds(40)
        maxFrameSize = 256L * 1024L
        masking = false
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            if (cause is kotlinx.coroutines.CancellationException) throw cause
            call.application.log.error("OneChat server request failed", cause)
            call.respond(HttpStatusCode.InternalServerError, ApiError("Internal server error"))
        }
    }
    if (config.allowedOrigins.isNotEmpty()) {
        install(CORS) {
            allowHeader(HttpHeaders.Authorization)
            allowHeader(HttpHeaders.ContentType)
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            for (origin in config.allowedOrigins) {
                val uri = URI(origin)
                val host = uri.host + if (uri.port >= 0) ":" + uri.port else ""
                allowHost(host, schemes = listOf(uri.scheme))
            }
        }
    }

    routing {
        get("/health") { call.respondText("ok") }
        get("/ready") {
            if (db.ready()) call.respondText("ready")
            else call.respond(HttpStatusCode.ServiceUnavailable, ApiError("Database unavailable"))
        }
        route("/v1") {
            post("/auth/register") {
                val req = call.receive<Credentials>()
                val username = req.username.trim()
                if (!limiter.allow("reg:" + username, 8)) {
                    call.respond(HttpStatusCode.TooManyRequests, ApiError("Slow down"))
                    return@post
                }
                if (!namePattern.matches(username) || req.password.toByteArray(Charsets.UTF_8).size !in 8..72) {
                    call.respond(HttpStatusCode.BadRequest, ApiError("Invalid username/password"))
                    return@post
                }
                if (req.inviteCode == null || !constantTimeEquals(req.inviteCode, config.inviteCode)) {
                    call.respond(HttpStatusCode.Forbidden, ApiError("Invalid invite code"))
                    return@post
                }
                val account = db.register(username, BCrypt.hashpw(req.password, BCrypt.gensalt(10)))
                if (account == null) {
                    call.respond(HttpStatusCode.Conflict, ApiError("Username already exists"))
                    return@post
                }
                call.response.header(HttpHeaders.CacheControl, "no-store")
                call.respond(HttpStatusCode.Created, AuthReply(account, jwt.issue(account)))
            }
            post("/auth/login") {
                val req = call.receive<Credentials>()
                val username = req.username.trim()
                if (!limiter.allow("login:" + username, 12)) {
                    call.respond(HttpStatusCode.TooManyRequests, ApiError("Slow down"))
                    return@post
                }
                val account = db.findWithHash(username)
                if (account == null || !BCrypt.checkpw(req.password, account.hash)) {
                    call.respond(HttpStatusCode.Unauthorized, ApiError("Invalid credentials"))
                    return@post
                }
                call.response.header(HttpHeaders.CacheControl, "no-store")
                call.respond(AuthReply(account.user, jwt.issue(account.user)))
            }
            get("/me") {
                val account = call.user(jwt, db) ?: return@get
                call.respond(account)
            }
            get("/users") {
                val account = call.user(jwt, db) ?: return@get
                val query = call.request.queryParameters["q"].orEmpty().trim()
                if (!Regex("[A-Za-z0-9_]{0,24}").matches(query)) {
                    call.respond(HttpStatusCode.BadRequest, ApiError("Invalid search"))
                    return@get
                }
                call.respond(db.findUsers(query, account.id))
            }
            post("/conversations/direct") {
                val account = call.user(jwt, db) ?: return@post
                val req = call.receive<DirectRequest>()
                val peer = db.findByName(req.username.trim())
                if (peer == null || peer.id == account.id) {
                    call.respond(HttpStatusCode.BadRequest, ApiError("Invalid peer"))
                    return@post
                }
                val id = db.directConversation(account.id, peer.id)
                call.respond(db.conversations(account.id).first { it.id == id })
            }
            get("/conversations") {
                val account = call.user(jwt, db) ?: return@get
                call.respond(db.conversations(account.id))
            }
            get("/conversations/{id}/messages") {
                val account = call.user(jwt, db) ?: return@get
                val id = call.parameters["id"].orEmpty()
                if (!db.isMember(account.id, id)) {
                    call.respond(HttpStatusCode.Forbidden, ApiError("Access denied"))
                    return@get
                }
                val rawLimit = call.request.queryParameters["limit"]
                val limit = if (rawLimit == null) 50 else rawLimit.toIntOrNull()
                if (limit == null || limit !in 1..100) {
                    call.respond(HttpStatusCode.BadRequest, ApiError("Invalid limit"))
                    return@get
                }
                val before = call.request.queryParameters["beforeId"]?.takeIf { it.isNotBlank() }
                call.respond(db.history(id, limit, before))
            }
            post("/conversations/{id}/messages") {
                val account = call.user(jwt, db) ?: return@post
                val id = call.parameters["id"].orEmpty()
                if (!db.isMember(account.id, id)) {
                    call.respond(HttpStatusCode.Forbidden, ApiError("Access denied"))
                    return@post
                }
                val request = call.receive<MessageRequest>()
                val body = request.body.trim()
                if (body.isBlank() || body.length > 4000 || !messageIdPattern.matches(request.clientMessageId)) {
                    call.respond(HttpStatusCode.BadRequest, ApiError("Invalid message"))
                    return@post
                }
                val created = try { db.send(id, account.id, request.clientMessageId, body) }
                catch (_: ChatDatabase.DuplicateIdConflict) {
                    call.respond(HttpStatusCode.Conflict, ApiError("clientMessageId already used"))
                    return@post
                }
                if (created.created) hub.emit(db.members(id), created.message)
                call.respond(if (created.created) HttpStatusCode.Created else HttpStatusCode.OK, created.message)
            }
            post("/ws-ticket") {
                val account = call.user(jwt, db) ?: return@post
                val ticket = tickets.issue(account.id)
                call.response.header(HttpHeaders.CacheControl, "no-store")
                call.respond(WsTicketReply(ticket, 60))
            }
            webSocket("/ws") {
                val who = tickets.consume(call.request.queryParameters["ticket"])
                if (who == null || db.findUser(who) == null) {
                    close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid ticket"))
                    return@webSocket
                }
                hub.join(who, this)
                try { for (frame in incoming) { if (frame is Frame.Close) break } }
                finally { hub.leave(who, this) }
            }
        }
    }
}
