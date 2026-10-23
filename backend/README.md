# OneChat preview — real Ktor/PostgreSQL backend

This is a **separate, real** HTTP/WebSocket service built alongside the AdaptiveKT
Maven Central UI verification branch. It does not replace the missing historical
OneChat checkout.

It needs PostgreSQL. Set:
- `ONECHAT_DATABASE_URL`: JDBC string like `jdbc:postgresql://host:5432/db`
- `ONECHAT_DB_USER`, `ONECHAT_DB_PASSWORD`
- `ONECHAT_JWT_SECRET`: at least 32 characters from a secure random generator
- `ONECHAT_INVITE_CODE`: at least 12 characters; mandatory for registrations
- `PORT`: HTTP port, default `8080`.

Only transport over HTTPS/WSS in a public environment. Do not commit credentials.
This preview has **no password reset**, **no rate-limiter distributed across instances**
and **no encrypted media transfer**. Do not use for sensitive conversations.

Run locally from this folder: `gradle -p backend run` (with a Postgres env).
Docker: `docker build -t onechat-preview .` (build context is this folder).

API:
- `GET /health` and `GET /ready`
- `POST /v1/auth/register` (requires invite code)
- `POST /v1/auth/login`
- `GET /v1/me`
- `GET /v1/users?q=...`
- `POST /v1/conversations/direct` with peer `username`
- `GET /v1/conversations`
- `GET /v1/conversations/{id}/messages?limit=50&beforeId=...`
- `POST /v1/conversations/{id}/messages` with `clientMessageId` (idempotent)
- `POST /v1/ws-ticket` -> one-time ticket, then `GET /v1/ws?ticket=...`.
  All new messages are persisted **before** broadcast, only members receive events.
  Sender posts through HTTP; WS is for server-side events and keepalive.

Continuous integration uses an ephemeral **actual PostgreSQL 16** service with
JWT/invite/authorization/idempotence/persistence tests. The state machine consumer
is separately tested under `validation/onechat-maven-alpha01`.

Deployment: place this folder as service root, use its Dockerfile, provide the
environment variables above and a PostgreSQL database; bind `PORT`.
Railway account trial expiration presently prevents new deployments even into its
existing VeilShare project; no paid plan is assumed.
