package com.onechat.client

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypted Android-only on-disk history.
 *
 * - AES-256-GCM via nonexportable AndroidKeyStore per-account key.
 * - Random 96-bit IV from the provider on every save, authenticated account/version AAD.
 * - AtomicFile completes/fails writes atomically; never silently resets a corrupted vault.
 * - App-private noBackupFilesDir, no plaintext journal/temp files and no auth/session tokens.
 * - File snapshot is intentionally bounded. Future version may migrate to encrypted SQLite.
 *
 * This is NOT E2EE. Render and Neon still receive/store message plaintext.
 */
internal class AndroidEncryptedChatStore(context: Context) : ChatLocalStore {
    private val directory = File(context.noBackupFilesDir, "onechat_private_v1")
    private val codec = Json { ignoreUnknownKeys = false; encodeDefaults = true }

    private fun keyId(userId: String): String {
        require(Regex("[a-fA-F0-9-]{36}").matches(userId)) { "Invalid account id" }
        val digest = MessageDigest.getInstance("SHA-256").digest(userId.encodeToByteArray())
        return digest.joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
    }
    private fun path(userId: String) = File(directory, keyId(userId) + ".ocv")
    private fun alias(userId: String) = "onechat_local_v1_" + keyId(userId)

    private fun findKey(userId: String): SecretKey? {
        val ks = KeyStore.getInstance("AndroidKeyStore")
        ks.load(null)
        return ks.getKey(alias(userId), null) as? SecretKey
    }

    private fun createKey(userId: String): SecretKey {
        findKey(userId)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                alias(userId),
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun aad(userId: String): ByteArray =
        ("OneChat|local-history|v1|" + userId).toByteArray(Charsets.UTF_8)

    override suspend fun restore(userId: String): LocalChatSnapshot = withContext(Dispatchers.IO) {
        val file = path(userId)
        // AtomicFile restores its .bak before reading if a previous write failed.
        val atomic = AtomicFile(file)
        if (!file.exists() && !File(file.path + ".bak").exists()) return@withContext LocalChatSnapshot()
        val key = findKey(userId) ?: throw SecurityException("Encrypted history key unavailable")
        val bytes = atomic.openRead().use { input ->
            // Avoid allocating unbounded memory for malformed files.
            if (file.length() > MAX_FILE_BYTES) throw SecurityException("Encrypted history is too large")
            input.readBytes()
        }
        require(bytes.size in (HEADER_BYTES + 16)..MAX_FILE_BYTES) { "Invalid encrypted history size" }
        require(bytes.copyOfRange(0, 4).contentEquals(MAGIC)) { "Unknown history format" }
        val iv = bytes.copyOfRange(4, 16)
        val encrypted = bytes.copyOfRange(HEADER_BYTES, bytes.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        cipher.updateAAD(aad(userId))
        val plain = cipher.doFinal(encrypted)
        val snapshot = codec.decodeFromString<LocalChatSnapshot>(plain.toString(Charsets.UTF_8))
        require(snapshot.messages.size <= 4000 && snapshot.conversations.size <= 300) {
            "Encrypted history exceeds allowed records"
        }
        require(snapshot.messages.all { it.conversationId.isNotBlank() }) { "Invalid cached message" }
        snapshot
    }

    override suspend fun save(userId: String, snapshot: LocalChatSnapshot) = withContext(Dispatchers.IO) {
        val limited = snapshot.bounded()
        val plain = codec.encodeToString(limited).toByteArray(Charsets.UTF_8)
        require(plain.size <= MAX_PLAIN_BYTES) { "Local history exceeds storage limit" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, createKey(userId))
        cipher.updateAAD(aad(userId))
        val payload = MAGIC + cipher.iv + cipher.doFinal(plain)
        require(payload.size <= MAX_FILE_BYTES) { "Encrypted history exceeds storage limit" }
        check(directory.exists() || directory.mkdirs()) { "Cannot create private history directory" }
        val atomic = AtomicFile(path(userId))
        val out = atomic.startWrite()
        try {
            out.write(payload)
            atomic.finishWrite(out)
        } catch (error: Exception) {
            atomic.failWrite(out)
            throw error
        }
    }

    private companion object {
        private val MAGIC = byteArrayOf(0x4f, 0x43, 0x56, 0x31) // OCV1
        private const val HEADER_BYTES = 16 // 4-byte magic + 12-byte GCM IV
        private const val MAX_PLAIN_BYTES = 4 * 1024 * 1024
        private const val MAX_FILE_BYTES = MAX_PLAIN_BYTES + 256
    }
}

@Composable
actual fun rememberChatLocalStore(): ChatLocalStore {
    val appContext = LocalContext.current.applicationContext
    return remember(appContext) { AndroidEncryptedChatStore(appContext) }
}
