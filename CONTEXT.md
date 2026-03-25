# Chat App (KMP Compose + Spring Boot) - Contexto

## Estructura
- `composeApp/`: App KMP con UI Compose Multiplatform (Android/Desktop/Wasm).
- `back/`: Servidor Spring Boot 3 + JPA (PostgreSQL) + WebSocket.

## Objetivo del MVP actual
1. Autenticación “solo Google” (MVP): el cliente intercambia un `idToken` contra el backend.
2. Chat funcional por REST + actualización en tiempo real vía WebSocket.

## Backend (`back/`)

### Endpoints REST relevantes
- `GET /api/check` -> `ok` (healthcheck).
- `GET /api/chats/{userId}` -> lista de `Chat` con `messages` y `participants`.
- `GET /api/messages/chat/{chatId}` -> lista de `Message` por chat.
- `POST /api/messages` -> envía un mensaje (recibe `MessageResponse`).
- `POST /api/auth/google/exchange`
  - Body: `{ "idToken": "..." }`
  - Valida el token contra Google (`https://oauth2.googleapis.com/tokeninfo?id_token=...`)
  - MVP adicional: si `tokeninfo` falla, intenta decodificar el JWT (para tokens tipo Firebase ID token) y extrae `email/name/picture`.
  - Crea/actualiza `User` por email y retorna `UserResponse`.

### WebSocket (tiempo real)
- URL: `ws://<serverHost>/ws` (o `wss://...`)
- Protocolo JSON (texto):
  - Suscribirse a un chat:
    - Client -> `{ "type": "subscribe", "chatId": 123 }`
  - Evento server -> `{ "type": "message", "chatId": 123, "message": { ... } }`
    - El `message` incluye `chatId` (propiedad calculada) para facilitar el update del cliente.

## Cliente (`composeApp/`)

### Flujo de login
- UI: `GoogleLoginScreen`
- En `wasmJs`/web: sign-in con Google vía Firebase Auth (en `index.html`) y se obtiene `idToken` automáticamente.
- En otros targets: queda el input manual de `idToken`.
- `AuthService.signInWithGoogle(idToken)` llama:
  - `POST /api/auth/google/exchange`
- El `User` se persiste localmente en `multiplatform-settings` y el app entra a `MainScreen`.

### WebSocket en UI
- `MainScreen` conecta al websocket y:
  - se suscribe únicamente al `selectedChat?.id` (MVP)
  - al recibir mensajes, actualiza `chats` y `selectedChat`.

## Notas / Limitaciones (MVP)
- “Estado de mensajes” (`sent/received/viewed/read`) no está completamente implementado en backend (por ahora el envío usa `sent`).
- Para “WebSocket de todos los chats” haría falta un canal por usuario o suscripciones múltiples; ahora es solo el chat activo.

