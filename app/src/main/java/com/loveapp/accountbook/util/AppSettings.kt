package com.loveapp.accountbook.util

import android.content.Context

object AppSettings {
    private const val PREF_NAME = "app_settings"
    private const val KEY_AUTO_BACKUP = "auto_backup"
    private const val KEY_NOTIFICATION = "notification_enabled"
    private const val KEY_DARK_MODE = "dark_mode_enabled"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun isAutoBackupEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_BACKUP, true)

    fun setAutoBackupEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO_BACKUP, enabled).apply()
    }

    fun isNotificationEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOTIFICATION, true)

    fun setNotificationEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOTIFICATION, enabled).apply()
    }

    fun isDarkModeEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DARK_MODE, false)

    fun setDarkModeEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }
}
