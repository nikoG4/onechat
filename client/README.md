# OneChat Kotlin Multiplatform client

This is the **connected OneChat preview client**, not an isolated AdaptiveKT sample.

**Backend:** `https://onechat-rl58.onrender.com` (Ktor, Neon PostgreSQL).

## Run

- Android: `gradle -p client assembleDebug`; install `client/build/outputs/apk/debug/client-debug.apk`. The APK is also uploaded by GitHub Actions to each successful run.
- Desktop: `gradle -p client run` (JDK 17).
- WebAssembly: `gradle -p client compileKotlinWasmJs`. A browser build requires a frontend host and correct server CORS allowlist (`ONECHAT_ALLOWED_ORIGINS`); it is not yet published as a public web app.

The application entry points are `MainActivity`, `DesktopMain` and `WebMain`. UI is `com.onechat.client.OneChatApp`, backed by `OneChatController` and `OneChatApi`.

## Flows

- Register (username, password, server invitation code) or log in.
- Search another registered username and open/create direct conversation.
- Browse conversations and recent message history.
- Send messages with a stable client ID; reattempt the same message after uncertain HTTP failure without duplication.
- Connect to the authenticated WebSocket using a single-use 60-second ticket and refresh history after reconnection.

AdaptiveKT is consumed strictly as `io.github.nikog4.adaptivekt:adaptive-*:0.1.0-alpha01` from Maven Central. There are no source or composite-build dependencies on the AdaptiveKT repository.

## Verification

CI checks Android/Desktop/wasmJs compilation, builds the Android debug APK, executes pure message/outbox/reconnect tests and the HTTP contract tests using Ktor MockEngine.

## Limitations

Credentials and JWT are kept in memory only. Closing the app requires login again. No encrypted local persistence or durable outbox across process death, end-to-end encryption, media transfer, password recovery or push notifications. Browser deployment and end-to-end UI testing on physical devices are still pending.
