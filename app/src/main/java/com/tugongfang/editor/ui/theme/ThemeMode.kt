package com.tugongfang.editor.ui.theme

import android.content.Context

enum class ThemeMode(val label: String) {
    LIGHT("浅色"), DARK("深色");
    companion object {
        fun from(value: String?): ThemeMode = entries.firstOrNull { it.name == value } ?: LIGHT
    }
}

/** UI preference only; never stored inside an editable photo project. */
class ThemeModeStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("tgf_ui_settings", Context.MODE_PRIVATE)
    fun read(): ThemeMode = ThemeMode.from(preferences.getString("theme_mode", "LIGHT"))
    fun write(mode: ThemeMode) { preferences.edit().putString("theme_mode", mode.name).apply() }
}
