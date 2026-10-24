package com.onechat.mesh

/**
 * Pure KMP routing core. No Bluetooth, network access or filesystem.
 *
 * This type must carry only E2EE ciphertext after the cryptographic layer
 * exists; the existing OneChat plaintext API MUST NOT be bridged through it.
 */
enum class MeshTransportKind { INTERNET, BLE, ESP_NOW, LORA }

enum class MeshPeerKind { MOBILE, DEDICATED_NODE }

data class MeshEnvelope(
    val id: String,
    /** Opaque recipient routing tag; never a username or MAC address. */
    val recipientTag: String,
    val encryptedPayload: ByteArray,
    val createdAtMillis: Long,
    val expiresAtMillis: Long,
    val hopsRemaining: Int,
)

sealed interface MeshDecision {
    data class Deliver(val envelope: MeshEnvelope) : MeshDecision
    data class Forward(val envelope: MeshEnvelope) : MeshDecision
    data class Drop(val reason: DropReason) : MeshDecision
}

enum class DropReason { INVALID_PACKET, EXPIRED, DUPLICATE, HOP_LIMIT }

/**
 * A phone and a dedicated ESP32 are both first-class relays. Nothing here
 * assumes that the previous/next hop is an ESP32 or a gateway.
 *
 * The exact same envelope can arrive via BLE, ESP-NOW, LoRa or Internet.
 * The transport determines per-hop fragmentation/acknowledgement separately.
 * Cache is intentionally memory-only; store-and-forward persistence comes
 * after authenticated, encrypted packet formats and storage policies are set.
 */
class MeshRelay(
    private val maxSeen: Int = 2048,
    private val maxPayloadBytes: Int = 4096,
    private val maxHops: Int = 16,
) {
    init {
        require(maxSeen > 0 && maxPayloadBytes > 0 && maxHops > 0)
    }

    private val seenUntil = LinkedHashMap<String, Long>()

    fun accept(
        envelope: MeshEnvelope,
        localRecipientTags: Set<String>,
        nowMillis: Long,
    ): MeshDecision {
        // This is input validation and loop suppression, not authentication.
        if (envelope.id.length !in 8..128 ||
            envelope.recipientTag.length !in 8..128 ||
            envelope.encryptedPayload.isEmpty() ||
            envelope.encryptedPayload.size > maxPayloadBytes ||
            envelope.createdAtMillis > nowMillis + 5L * 60L * 1000L ||
            envelope.expiresAtMillis <= envelope.createdAtMillis ||
            envelope.hopsRemaining !in 0..maxHops
        ) return MeshDecision.Drop(DropReason.INVALID_PACKET)

        if (envelope.expiresAtMillis <= nowMillis) return MeshDecision.Drop(DropReason.EXPIRED)

        // Keep the deduplication cache bounded, even on a node with no disk.
        val iter = seenUntil.entries.iterator()
        while (iter.hasNext()) {
            if (iter.next().value <= nowMillis) iter.remove()
        }
        if (seenUntil.containsKey(envelope.id)) return MeshDecision.Drop(DropReason.DUPLICATE)

        seenUntil[envelope.id] = envelope.expiresAtMillis
        while (seenUntil.size > maxSeen) {
            val oldest = seenUntil.keys.firstOrNull() ?: break
            seenUntil.remove(oldest)
        }

        // A phone may itself be the destination; it does not need a BLE->ESP bridge.
        if (envelope.recipientTag in localRecipientTags) return MeshDecision.Deliver(envelope)

        if (envelope.hopsRemaining == 0) return MeshDecision.Drop(DropReason.HOP_LIMIT)
        return MeshDecision.Forward(envelope.copy(hopsRemaining = envelope.hopsRemaining - 1))
    }
}
