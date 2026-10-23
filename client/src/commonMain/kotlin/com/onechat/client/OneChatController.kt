package com.onechat.client

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.onechat.validation.ConnectionLabel
import com.onechat.validation.OneChatBubble
import com.onechat.validation.OneChatConversation
import com.onechat.validation.OneChatView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Session-scoped state holder. Tokens/passwords remain in memory; no client-side mock data.
 * All updates occur on the caller's Compose/UI coroutine scope.
 */
class OneChatController(
    private val api: OneChatApi,
    private val scope: CoroutineScope,
    private val localStore: ChatLocalStore = EphemeralChatStore(),
) : AutoCloseable {
    var account by mutableStateOf<ApiUser?>(null)
        private set
    var view by mutableStateOf(OneChatView())
        private set
    var authBusy by mutableStateOf(false)
        private set
    var authError by mutableStateOf<String?>(null)
        private set
    var serverStatus by mutableStateOf<String?>(null)
        private set
    var serverBusy by mutableStateOf(false)
        private set
    var newConversation by mutableStateOf(false)
        private set
    var recipientQuery by mutableStateOf("")
        private set
    var matchingUsers by mutableStateOf<List<ApiUser>>(emptyList())
        private set
    var findingUser by mutableStateOf(false)
        private set
    var storageWarning by mutableStateOf<String?>(null)
        private set

    private var accessToken: String? = null
    private var epoch = 0L
    private var selectionEpoch = 0L
    private var wsJob: Job? = null
    private var persistJob: Job? = null
    private var storageWritable = false
    private var knownConversations: List<ApiConversation> = emptyList()
    private val cache = mutableMapOf<String, ApiMessage>()
    private val pending = mutableMapOf<String, MessageInput>()

    /** A safe unauthenticated probe for diagnosing real network failures on the device. */
    fun checkServer() {
        if (serverBusy) return
        serverBusy = true
        serverStatus = "Comprobando conexión con Render y PostgreSQL…"
        val expected = epoch
        scope.launch {
            try {
                val ready = api.health()
                if (epoch == expected) serverStatus = if (ready)
                    "Servidor disponible: Render responde y PostgreSQL está accesible."
                else "Render responde, pero el backend todavía no está listo (/ready no respondió 200)."
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                if (epoch == expected) serverStatus = safeMessage(e)
            } finally {
                if (epoch == expected) serverBusy = false
            }
        }
    }

    fun authenticate(username: String, password: String, invite: String?, createAccount: Boolean) {
        if (authBusy) return
        if (username.isBlank() || password.isBlank()) {
            authError = "Ingresá usuario y contraseña."
            return
        }
        if (createAccount) {
            SignupValidation.error(username, password, invite.orEmpty())?.let { problem ->
                authError = problem
                return
            }
        }
        authBusy = true
        authError = null
        serverStatus = null
        val expected = ++epoch
        scope.launch {
            try {
                val response = if (createAccount) api.register(username.trim(), password, invite.orEmpty())
                else api.login(username.trim(), password)
                if (expected != epoch) return@launch
                accessToken = response.token
                account = response.user
                storageWritable = false
                storageWarning = null
                view = OneChatView(connection = ConnectionLabel.CONNECTING, loading = true)
                // Persisted Android cache is opened only AFTER server-authenticated login.
                restoreLocalHistory(response.user.id, expected)
                if (expected != epoch) return@launch
                refresh()
                connectRealtime(expected, response.token)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                if (expected == epoch) authError = safeMessage(e)
            } finally {
                if (expected == epoch) authBusy = false
            }
        }
    }

    fun signOut() {
        epoch++
        selectionEpoch++
        wsJob?.cancel()
        wsJob = null
        persistJob?.cancel()
        persistJob = null
        storageWritable = false
        knownConversations = emptyList()
        storageWarning = null
        accessToken = null
        account = null
        cache.clear()
        pending.clear()
        newConversation = false
        matchingUsers = emptyList()
        view = OneChatView()
        authError = null
        authBusy = false
        serverBusy = false
        serverStatus = null
    }

    private suspend fun restoreLocalHistory(userId: String, expected: Long) {
        try {
            val snapshot = localStore.restore(userId)
            if (expected != epoch) return
            knownConversations = snapshot.conversations
            cache.clear()
            snapshot.messages.forEach { cache[it.id] = it }
            view = view.copy(
                conversations = knownConversations.map {
                    OneChatConversation(it.id, it.peer.username, it.preview.orEmpty())
                },
            )
            updateTimeline()
            storageWritable = true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Fail closed. Never overwrite an unreadable/corrupt local vault.
            if (expected == epoch) {
                storageWritable = false
                storageWarning = "El historial local cifrado no se pudo abrir. " +
                    "Está protegido de sobrescritura; podés seguir usando el servidor."
            }
        }
    }

    /** Coalesce bursts of HTTP history and WebSocket events into bounded encrypted snapshots. */
    private fun schedulePersist() {
        val user = account ?: return
        if (!storageWritable) return
        val snapshot = LocalChatSnapshot(
            conversations = knownConversations.toList(),
            messages = cache.values.toList(),
        ).bounded()
        val expected = epoch
        persistJob?.cancel()
        persistJob = scope.launch {
            delay(350)
            try {
                localStore.save(user.id, snapshot)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (epoch == expected) {
                    storageWritable = false
                    storageWarning = "No se pudo guardar el historial cifrado. " +
                        "El modo local quedó suspendido para evitar pérdida de datos."
                }
            }
        }
    }

    private fun updateTimeline() {
        val owner = account?.id
        view = view.copy(messages = cache.values
            .sortedWith(compareBy<ApiMessage> { it.createdAt }.thenBy { it.id })
            .map {
                OneChatBubble(
                    id = it.id,
                    conversationId = it.conversationId,
                    body = it.body,
                    mine = it.senderId == owner,
                    label = if (it.senderId == owner) "Enviado" else "Recibido",
                )
            })
    }

    fun refresh() {
        val token = accessToken ?: return
        val expected = epoch
        scope.launch {
            view = view.copy(loading = true, error = null)
            try {
                val list = api.conversations(token)
                if (epoch != expected) return@launch
                knownConversations = list
                view = view.copy(
                    conversations = list.map {
                        OneChatConversation(it.id, it.peer.username, it.preview.orEmpty())
                    },
                    loading = false,
                    error = null,
                )
                schedulePersist()
                val selected = view.selectedId
                if (selected != null) loadHistory(selected)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                if (epoch == expected) view = view.copy(loading = false, error = safeMessage(e))
            }
        }
    }

    fun select(id: String) {
        selectionEpoch++
        view = view.copy(selectedId = id, query = "", error = null)
        loadHistory(id)
    }

    fun back() {
        selectionEpoch++
        view = view.copy(selectedId = null)
    }

    fun query(value: String) { view = view.copy(query = value) }
    fun draft(value: String) { view = view.copy(draft = value) }

    private fun loadHistory(id: String) {
        val token = accessToken ?: return
        val expected = epoch
        val selection = selectionEpoch
        scope.launch {
            try {
                val messages = api.history(token, id)
                if (epoch != expected || selection != selectionEpoch || view.selectedId != id) return@launch
                mergeMessages(messages)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                if (epoch == expected && selection == selectionEpoch) {
                    view = view.copy(error = safeMessage(e))
                }
            }
        }
    }

    fun send(conversationId: String, body: String) {
        val token = accessToken ?: return
        val trimmed = body.trim()
        if (trimmed.isEmpty() || view.sending) return
        if (trimmed.length > 4000) {
            view = view.copy(error = "Máximo 4000 caracteres por mensaje.")
            return
        }
        // Keep the exact client ID after a transport failure so a retry is idempotent.
        val previous = pending[conversationId]
        val input = if (previous != null && previous.body == trimmed) previous
        else MessageInput(newClientMessageId(), trimmed).also { pending[conversationId] = it }
        val expected = epoch
        view = view.copy(sending = true, error = null)
        scope.launch {
            try {
                val server = api.send(token, conversationId, input)
                if (epoch != expected) return@launch
                pending.remove(conversationId)
                mergeMessages(listOf(server))
                if (view.selectedId == conversationId && view.draft.trim() == trimmed) {
                    view = view.copy(draft = "")
                }
                refresh()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                if (epoch == expected) view = view.copy(
                    error = safeMessage(e) + " Volvé a enviar para reintentar sin duplicar."
                )
            } finally {
                if (epoch == expected) view = view.copy(sending = false)
            }
        }
    }

    fun showNewConversation() {
        newConversation = true
        recipientQuery = ""
        matchingUsers = emptyList()
        findingUser = false
    }
    fun dismissNewConversation() { newConversation = false }
    fun recipientQuery(value: String) {
        recipientQuery = value
        matchingUsers = emptyList()
    }
    fun findRecipients() {
        val token = accessToken ?: return
        val query = recipientQuery.trim()
        if (query.length !in 1..24 || !query.all { it.isLetterOrDigit() || it == '_' }) {
            view = view.copy(error = "Buscá por nombre de usuario.")
            return
        }
        findingUser = true
        val expected = epoch
        scope.launch {
            try {
                val result = api.search(token, query)
                if (expected == epoch && recipientQuery.trim() == query) matchingUsers = result
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                if (expected == epoch) view = view.copy(error = safeMessage(e))
            } finally {
                if (expected == epoch) findingUser = false
            }
        }
    }

    fun startConversation(peer: ApiUser) {
        val token = accessToken ?: return
        val expected = epoch
        scope.launch {
            try {
                val created = api.direct(token, peer.username)
                if (epoch != expected) return@launch
                newConversation = false
                view = view.copy(error = null)
                select(created.id)
                refresh()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                if (expected == epoch) view = view.copy(error = safeMessage(e))
            }
        }
    }

    private fun connectRealtime(expected: Long, token: String) {
        wsJob?.cancel()
        wsJob = scope.launch {
            var retry = 1500L
            while (epoch == expected) {
                view = view.copy(connection = ConnectionLabel.CONNECTING)
                try {
                    api.events(token, onConnected = {
                        if (epoch == expected) {
                            view = view.copy(connection = ConnectionLabel.ONLINE, error = null)
                            retry = 1500L
                            refresh() // Catch messages missed between HTTP load and socket open.
                        }
                    }) { event ->
                        if (epoch == expected) {
                            mergeMessages(listOf(event.message))
                            refresh() // Updates conversation previews even for unselected chats.
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // No token or URL in user-visible errors.
                }
                if (epoch != expected) return@launch
                view = view.copy(connection = ConnectionLabel.RECONNECTING)
                delay(retry)
                retry = (retry * 2).coerceAtMost(20_000L)
            }
        }
    }

    private fun mergeMessages(messages: List<ApiMessage>) {
        for (message in messages) cache[message.id] = message
        updateTimeline()
        schedulePersist()
    }

    override fun close() {
        signOut()
        api.close()
    }
}

private fun safeMessage(e: Exception): String = when (e) {
    is OneChatHttpException -> when (e.code) {
        400 -> "Registro inválido: el usuario debe tener 3-24 caracteres y la contraseña entre 8 y 72 bytes UTF-8."
        401 -> "Usuario o contraseña incorrectos, o sesión expirada."
        403 -> "Acceso denegado o código de invitación incorrecto."
        404 -> "Usuario o conversación inexistente."
        409 -> "Ya existe un registro con esos datos."
        429 -> "Demasiadas solicitudes; probá más tarde."
        else -> "Error del servidor (HTTP " + e.code + ")."
    }
    else -> {
        // Differentiate timeout, TLS/DNS and JSON parsing instead of mislabeling all exceptions
        // as network errors. Do not print request URLs, passwords or WebSocket tickets.
        val kind = e::class.simpleName.orEmpty()
        when {
            kind.contains("Timeout", ignoreCase = true) ->
                "Tiempo de espera agotado al conectar. Render Free podría estar despertando; volvé a intentar."
            kind.contains("Serialization", ignoreCase = true) ||
                kind.contains("JsonConvert", ignoreCase = true) ->
                "El servidor devolvió una respuesta que la aplicación no pudo interpretar ($kind)."
            kind.contains("SSL", ignoreCase = true) || kind.contains("Certificate", ignoreCase = true) ->
                "Fallo TLS/HTTPS en este dispositivo ($kind)."
            kind.contains("Host", ignoreCase = true) || kind.contains("DNS", ignoreCase = true) ->
                "No se puede resolver el dominio de Render ($kind)."
            else -> "No se pudo completar la solicitud ($kind). Probá el diagnóstico del servidor."
        }
    }
}
