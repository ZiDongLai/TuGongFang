package com.tugongfang.editor.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {
    @Test fun onlyTwoModesRemain() {
        assertEquals(listOf(ThemeMode.LIGHT, ThemeMode.DARK), ThemeMode.entries.toList())
        assertEquals(ThemeMode.LIGHT, ThemeMode.from("LIGHT"))
        assertEquals(ThemeMode.DARK, ThemeMode.from("DARK"))
    }
    @Test fun legacySystemSettingMigratesToLight() {
        assertEquals(ThemeMode.LIGHT, ThemeMode.from("SYSTEM"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.from(null))
        assertEquals(ThemeMode.LIGHT, ThemeMode.from("unexpected"))
    }
}
