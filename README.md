# OneChat

Independent Kotlin Multiplatform messenger preview using **AdaptiveKT from Maven Central**, a real Ktor server and Neon PostgreSQL.

## Live backend

- API base: https://onechat-rl58.onrender.com
- Readiness: https://onechat-rl58.onrender.com/ready
- Docker service: Render Free, region Virginia; may sleep on inactivity.
- PostgreSQL 16: separate Neon project `onechat-preview`.

## Client targets

`client/` contains an actual Compose application for Android, desktop and wasmJs, with:
- registration using a private invitation code and sign-in using existing account;
- contact lookup and direct conversation creation;
- conversations, message history and idempotent outgoing HTTP messages;
- authenticated one-time WebSocket ticket, event delivery and reconnection with history refresh;
- layout using AdaptiveKT 0.1.0-alpha01 published on Maven Central.

Build an Android APK: `gradle -p client assembleDebug` (JDK 17 + Android SDK 35).

Launch desktop: `gradle -p client run` (JDK 17).

Compile wasmJs: `gradle -p client compileKotlinWasmJs`. Browser execution requires a frontend host and `ONECHAT_ALLOWED_ORIGINS` configured on the backend to permit that host.

GitHub Actions validates the Android, desktop and wasmJs compilation, tests the HTTP client against a mock transport and publishes the Android debug APK as a downloadable workflow artifact.

## Backend

`backend/` is a standalone Ktor 2.3.13 JVM service with:
- BCrypt credential hashing, JWT tokens, invitation-only registration;
- conversation membership checks and PostgreSQL persistence;
- idempotent `clientMessageId` and paginated history;
- per-user WebSocket delivery after a one-time ticket.

CI runs integration tests against actual PostgreSQL 16.

Set backend environment in Render secrets: `ONECHAT_DATABASE_URL` (JDBC, SSL), `ONECHAT_DB_USER`, `ONECHAT_DB_PASSWORD`, `ONECHAT_JWT_SECRET`, `ONECHAT_INVITE_CODE` and optional `ONECHAT_ALLOWED_ORIGINS`.

## Preview limitations

This is not production-ready secure messaging. No end-to-end encryption, media, push notifications, password reset, multi-device session synchronization, durable outbox across app restarts or persisted login. Render Free can cold-start after inactivity. The wasmJs browser bundle is not independently hosted yet, and cross-origin requests must be explicitly enabled for its eventual origin.

The previous Spring/Compose prototype is retired. Only this repository owns OneChat; AdaptiveKT lives in its separate library repository.
