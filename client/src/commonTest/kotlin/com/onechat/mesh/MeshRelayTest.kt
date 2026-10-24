package com.onechat.mesh

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MeshRelayTest {
    private fun msg(
        id: String = "message-0001",
        recipient: String = "opaque-phone-b",
        hops: Int = 4,
        expiry: Long = 10_000,
    ) = MeshEnvelope(
        id = id,
        recipientTag = recipient,
        encryptedPayload = byteArrayOf(1, 2, 3),
        createdAtMillis = 100,
        expiresAtMillis = expiry,
        hopsRemaining = hops,
    )

    @Test
    fun phoneToPhoneDirectBleDoesNotRequireBridge() {
        val relay = MeshRelay()
        assertIs<MeshDecision.Deliver>(
            relay.accept(msg(), setOf("opaque-phone-b"), nowMillis = 1000)
        )
    }

    @Test
    fun phoneRelaysToAnotherPhoneWithDecrementedHopCount() {
        val relay = MeshRelay()
        val decision = relay.accept(msg(), setOf("opaque-phone-middle"), nowMillis = 1000)
        assertEquals(3, assertIs<MeshDecision.Forward>(decision).envelope.hopsRemaining)
    }

    @Test
    fun sameMessageArrivingViaSeveralTransportsIsDeduplicated() {
        val relay = MeshRelay()
        assertIs<MeshDecision.Forward>(relay.accept(msg(), emptySet(), 1000))
        assertEquals(DropReason.DUPLICATE,
            assertIs<MeshDecision.Drop>(relay.accept(msg(), emptySet(), 1001)).reason)
    }

    @Test
    fun dropsExpiredAndHopExhaustedMessages() {
        val relay = MeshRelay()
        assertEquals(DropReason.EXPIRED,
            assertIs<MeshDecision.Drop>(relay.accept(msg(expiry = 500), emptySet(), 1000)).reason)
        assertEquals(DropReason.HOP_LIMIT,
            assertIs<MeshDecision.Drop>(relay.accept(msg(hops = 0), emptySet(), 1000)).reason)
    }

    @Test
    fun ignoresOversizedPacketsAndAllowsExpiredDedupEntriesToEvict() {
        val relay = MeshRelay(maxSeen = 1, maxPayloadBytes = 3)
        assertEquals(DropReason.INVALID_PACKET,
            assertIs<MeshDecision.Drop>(
                relay.accept(msg().copy(encryptedPayload = ByteArray(4)), emptySet(), 1000)
            ).reason)
        assertIs<MeshDecision.Forward>(
            relay.accept(msg(id = "message-0001"), emptySet(), 1000)
        )
        assertIs<MeshDecision.Forward>(
            relay.accept(msg(id = "message-0002"), emptySet(), 1000)
        )
        assertIs<MeshDecision.Forward>(
            relay.accept(msg(id = "message-0001"), emptySet(), 1001)
        )
    }
}
