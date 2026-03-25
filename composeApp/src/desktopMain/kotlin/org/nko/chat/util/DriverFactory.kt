package org.nko.chat.util

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.nko.chat.db.ChatDatabase
import java.io.File

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        val databaseFile = File(System.getProperty("user.home"), "chat.db")
        val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath}")
        if (!databaseFile.exists()) {
            ChatDatabase.Schema.create(driver)
        }
        return driver
    }
}
