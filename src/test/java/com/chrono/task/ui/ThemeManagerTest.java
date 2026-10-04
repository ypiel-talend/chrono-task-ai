package com.chrono.task.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ThemeManagerTest {

    @Test
    void isDarkDefaultsToDark() {
        assertTrue(ThemeManager.isDark(null));
        assertTrue(ThemeManager.isDark(""));
        assertTrue(ThemeManager.isDark("garbage"));
        assertTrue(ThemeManager.isDark("DARK"));
        assertFalse(ThemeManager.isDark("LIGHT"));
        assertFalse(ThemeManager.isDark(" light "));
    }

    @Test
    void markdownCssUsesFontAndThemeColors() {
        String dark = ThemeManager.markdownCss(true, "'Inter'");
        String light = ThemeManager.markdownCss(false, "sans-serif");

        assertTrue(dark.contains("font-family: 'Inter'"));
        assertTrue(dark.contains("#0d1117"));
        assertTrue(light.contains("font-family: sans-serif"));
        assertTrue(light.contains("#ffffff"));
        assertFalse(light.contains("#0d1117"));
        assertTrue(dark.contains(".edit-icon"));
    }
}
