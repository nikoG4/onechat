# ADR-002 — Phone-first, heterogeneous OneChat Mesh

Status: architecture accepted, initial pure-KMP relay core implemented; **no physical BLE, LoRa or ESP-NOW adapter yet**.

## Non-negotiable requirements

1. Two compatible **Android phones communicate directly over BLE**, offline and **without any ESP32 or gateway**.
2. A phone may simultaneously be a messaging endpoint and a mesh relay to other phones.
3. Dedicated nodes (ESP32 or other hardware) are optional participants, not mandatory phone bridges.
4. An ESP32 can bridge BLE ↔ ESP-NOW or BLE ↔ LoRa if it has the required LoRa transceiver. Phones cannot speak ESP-NOW natively through standard Android Bluetooth/Wi-Fi APIs.
5. Every packet must use the same OneChat encrypted, transport-agnostic envelope. No plaintext chat payloads are allowed through the mesh.
6. Internet/Render remains an optional additional path, not a mesh dependency.

## Android BLE architecture

- Advertise a OneChat service UUID with minimal pseudonymous rotating discovery data.
- Scan for peer service UUIDs. Android acts both as a BLE **peripheral/GATT server** and **central/GATT client** on devices supporting advertising.
- Coordinate connections to prevent both phones initiating duplicate client GATT sessions simultaneously; employ a deterministic negotiated tie-break and reconnection/backoff.
- BLE-only phones without LE advertising support cannot be full symmetric peers. Surface capabilities and fallbacks clearly; do not claim universal Android compatibility.
- Exchange authenticated handshakes and encrypted envelopes over GATT characteristics for packet TX/RX, acknowledgements and protocol capabilities. Negotiate MTU; fragment/reassemble, CRC/checksum for transport errors, cap memory usage and timeouts.
- Receiving a message addressed to another local contact **does not decrypt it**; relay with bounded TTL, expiry, deduplication and controlled retry. Authenticated end-to-end delivery receipts are distinct from BLE write ACKs.
- Nearby device permissions (Android 12+) include BLUETOOTH_SCAN, BLUETOOTH_ADVERTISE and BLUETOOTH_CONNECT; Android 11 and older have legacy Bluetooth/location requirements.
- Background relay cannot be guaranteed indefinitely: use an explicit, opt-in foreground service of type connectedDevice with a persistent notification, compatible with modern Android background limits and battery policy. Test manufacturer differences and expose a suspend switch.

## Heterogeneous topology examples

- PHONE A --BLE--> PHONE B (direct)
- PHONE A --BLE--> PHONE B --BLE--> PHONE C (all mobile nodes)
- PHONE A --BLE--> ESP32 A --ESP-NOW--> ESP32 B --BLE--> PHONE B
- PHONE A --BLE--> ESP32+LoRa A --LoRa--> ESP32+LoRa B --BLE--> PHONE B
- PHONE A --Internet--> Render --Internet--> PHONE B

One node may support multiple transports. Route selection must not introduce a fixed ESP32 assumption.

## Implementation ordering and safeguards

- Phase A: shared mesh relay envelope and unit tests (this PR). This is loop/dedup scaffolding, not secure communication.
- Phase B: Android central+peripheral GATT dual-role proof-of-concept on two physical Android phones; automatic discovery, permission UI, single-hop direct transfer using synthetic *non-sensitive* test data.
- Phase C: authenticated transport, E2EE integration and account-to-node identity association, replay protection, per-hop ack, end-to-end receipt, BLE mesh on 3+ Android phones.
- Phase D: Android encrypted store-and-forward queue, bounded relay policy, foreground-service opt-in, responsible resource usage.
- Phase E: ESP32 BLE adapter + ESP-NOW bridge; LoRa adapter; optional Internet bridging and route prioritization.
- Phase F: cross-platform architecture. Desktop adapter when supported; wasmJs should not be marketed as a reliable BLE relay (Web Bluetooth has limited browser support and primarily client APIs).

## Security

The server version currently stores readable messages in PostgreSQL. This ADR must **not** be used as a pretext for routing those plaintext messages across untrusted relays. The mesh must remain opt-in/disabled for real chat content until compatible authenticated E2EE is in place.

An encrypted packet also leaks timing, size and connectivity metadata. Consider rotating discovery identifiers, caps on advertisements, TTL and forwarding limits, user opt-in and anti-spam defenses.
