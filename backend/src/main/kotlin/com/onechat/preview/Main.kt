package com.onechat.preview

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    val config = PreviewConfig.fromEnv()
    val database = ChatDatabase(config)
    Runtime.getRuntime().addShutdownHook(Thread { database.close() })
    val port = System.getenv("PORT")?.toIntOrNull()?.takeIf { it in 1..65535 } ?: 8080
    embeddedServer(Netty, host = "0.0.0.0", port = port) {
        installOneChatPreview(config, database)
    }.start(wait = true)
}
