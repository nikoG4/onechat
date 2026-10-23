package com.onechat.preview

import java.net.URI

data class PreviewConfig(
    val jdbcUrl: String,
    val dbUser: String,
    val dbPassword: String,
    val jwtSecret: String,
    val inviteCode: String,
    val allowedOrigins: List<String> = emptyList(),
) {
    init {
        require(jdbcUrl.startsWith("jdbc:postgresql://")) { "PostgreSQL JDBC URL required" }
        require(jwtSecret.length >= 32) { "JWT secret must contain at least 32 characters" }
        require(inviteCode.length >= 12) { "Invite code must contain at least 12 characters" }
        for (origin in allowedOrigins) {
            val uri = URI(origin)
            require(uri.scheme in listOf("http","https") && uri.host != null &&
                uri.rawPath.isNullOrEmpty() && uri.rawQuery == null &&
                uri.rawFragment == null && uri.userInfo == null) { "CORS origins must be bare http(s) origins" }
        }
    }
    companion object {
        fun fromEnv(): PreviewConfig {
            fun required(name: String): String =
                System.getenv(name)?.takeIf { it.isNotBlank() } ?: error("Missing $name")
            return PreviewConfig(
                jdbcUrl = required("ONECHAT_DATABASE_URL"),
                dbUser = required("ONECHAT_DB_USER"),
                dbPassword = required("ONECHAT_DB_PASSWORD"),
                jwtSecret = required("ONECHAT_JWT_SECRET"),
                inviteCode = required("ONECHAT_INVITE_CODE"),
                allowedOrigins = System.getenv("ONECHAT_ALLOWED_ORIGINS")
                    ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
            )
        }
    }
}
