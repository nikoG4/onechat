# ADR-001 — Encrypted local history: security boundaries

Status: Phase 1 implemented. This is encryption at rest on Android only, NOT E2EE.

## Platform policy

- Android: encrypted bounded snapshot in app-private noBackupFilesDir, per-account key stored in AndroidKeyStore. Limit 4000 messages, 300 conversations, 4 MiB JSON. This is not SQLite; encrypted SQLite/database migration is future work.
- Desktop: memory-only current messages. No persistent history or JWT.
- Wasm browser: memory-only current messages. No IndexedDB, localStorage, service worker message cache or persistent token. Browser/OS memory swapping is outside app control.

## Android local cryptographic format

- Each server-authenticated account UUID has its own 256-bit nonexportable AndroidKeyStore AES key.
- AES-256-GCM, provider-generated fresh 96-bit nonce per save, 128-bit tag.
- Authenticated additional data binds format version and account ID (cross-account substitution rejected).
- File format OCV1 magic, IV, authenticated ciphertext; no plaintext staging files.
- Android AtomicFile write/commit/rollback prevents torn commits. Missing keys, tampering or invalid format fail closed; client will not overwrite the unreadable vault.
- A successfully authenticated login is required before opening a user snapshot. Password and JWT never persist.
- Logout clears in-memory state but does not destroy previous encrypted device history. Uninstall or hardware key loss may make it unrecoverable.

## Limits and caveats

**Render and Neon still see and store messages in plaintext!** TLS only protects data in transit. This phase does not provide end-to-end encryption, metadata privacy, fully encrypted backups, process-memory safety or offline authentication.
Bulk encrypted snapshot writes are bounded and debounced. A true encrypted transactional database should replace snapshots before long-term deployment.

## Next milestones

1. On-device tests for restart/corruption/key loss and encrypted database strategy; UI for clearing local vault.
2. Audit proven multi-device E2EE protocols/libraries for Kotlin/JVM/Android/Wasm. Do not invent a protocol.
3. Independently authorized device keys, QR pairing, revocation and recovery keys. Do not transmit AndroidKeyStore material.
4. Ciphertext-only server queues with expiration, acknowledgement and explicit migration away from plaintext Neon rows.
5. Encrypt and transfer media via a separately designed offline-capable path. Photos are deferred in this phase.
6. No plaintext leakage via push notifications; privacy-focused notification behavior.

## Acceptance criteria

- Android encrypts every persisted snapshot before writing it to noBackupFilesDir.
- Unknown format, missing Keystore key or invalid GCM authentication tag never silently becomes an empty local history.
- Desktop and wasmJs storage implementations do not persist messages or credentials.
- CI compiles Android/Desktop/Wasm and runs common tests. This is not a replacement for physical-device security verification.