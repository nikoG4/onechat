package org.nko.chat.util

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import org.nko.chat.db.ChatDatabase

actual class DriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(ChatDatabase.Schema, context, "chat.db")
    }
}
