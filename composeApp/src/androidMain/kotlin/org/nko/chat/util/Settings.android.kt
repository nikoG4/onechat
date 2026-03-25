package org.nko.chat.util


import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import org.nko.chat.MainActivity

actual fun getSettings(): Settings {
    val sharedPreferences = MainActivity.appContext.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
    return SharedPreferencesSettings(sharedPreferences)
}
