package org.nko.chat.util

import app.cash.sqldelight.db.SqlDriver

expect class DriverFactory {
    fun createDriver(): SqlDriver
}
