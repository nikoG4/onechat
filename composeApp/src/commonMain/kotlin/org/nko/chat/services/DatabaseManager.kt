package org.nko.chat.services

import org.nko.chat.db.ChatDatabase
import org.nko.chat.util.DriverFactory

object DatabaseManager {
    private var database: ChatDatabase? = null

    fun init(driverFactory: DriverFactory) {
        if (database == null) {
            database = ChatDatabase(driverFactory.createDriver())
        }
    }

    val db: ChatDatabase
        get() = database ?: throw IllegalStateException("Database not initialized. Call init() first.")
}
