# OneChat Maven Central integration: isolated verification

This folder is a **reference consumer**, not the missing `onechat-kmp` checkout.
It deliberately uses published AdaptiveKT Maven Central **0.1.0-alpha01** and no
project-level module dependencies, snapshots, mavenLocal or source checkout.

## CI
`ci/onechat-maven-alpha01-smoke` triggers only its targeted workflow:
- `compileKotlinDesktop`
- `compileDebugKotlinAndroid`
- `compileKotlinWasmJs`
- `desktopTest` (JUnit)
  
## Domain implementation
- `ChatEngine`: creates optimistic message and stable clientMessageId outbox entry,
  merges WebSocket events and HTTP acknowledgement, queues offline messages, and
  prevents stale HTTP history GETs from clearing new WebSocket gaps or a selection
  change. The caller **must persist the state before performing I/O**, and restore
  outbox/sequence after restart; this project does not claim durable storage.
- `OneChatMavenWorkspace`: presentation only; Compose commonMain, AdaptiveKT controls,
  desktop 30/70 list/detail, compact single-pane; callbacks need the genuine
  OneChat ViewModels, repositories, and AppNavigator.
- Legacy `OneChatMavenConsumer` remains as an independent alpha API smoke.

## Not proven
The original OneChat backend Ktor, JWT, PostgreSQL, browser history,
cross-client WebSocket delivery, actual Android/Web browser runtime and Android
device runtime are not here. Tests are pure JVM logic plus cross-platform compile,
not end-to-end network or visual screenshot checks.

## Moving into real OneChat
Copy only vetted, non-conflicting commonMain state/UI into the real repo and
adapt to its existing DTOs. Do not replace authentication, navigator or session
store. Wire to real HTTP and WebSocket adaptors, then test with two accounts.
Run Gradle Android/Desktop/Wasm, Ktor tests and browser E2E on that checkout.
