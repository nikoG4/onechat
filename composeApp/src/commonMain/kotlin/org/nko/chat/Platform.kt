package org.nko.chat

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform