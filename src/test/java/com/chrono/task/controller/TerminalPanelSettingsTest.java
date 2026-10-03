package com.chrono.task.controller;

import com.chrono.task.model.Settings;
import com.kodedu.terminalfx.TerminalLook;
import com.kodedu.terminalfx.local.LocalShellSpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TerminalPanelSettingsTest {

    @Test
    void testDefaultShellSpec() {
        LocalShellSpec spec = TerminalPanel.buildShellSpec(new Settings());

        assertArrayEquals(LocalShellSpec.defaultShell().command(), spec.command());
        assertEquals(Path.of(System.getProperty("user.home")), spec.directory());
    }

    @Test
    void testCustomShellAndDirectory(@TempDir Path dir) {
        Settings settings = new Settings();
        settings.setTerminalShell("  /bin/sh   -l ");
        settings.setTerminalStartDirectory(dir.toString());

        LocalShellSpec spec = TerminalPanel.buildShellSpec(settings);

        assertArrayEquals(new String[]{"/bin/sh", "-l"}, spec.command());
        assertEquals(dir, spec.directory());
    }

    @Test
    void testMissingStartDirectoryFallsBackToHome() {
        Settings settings = new Settings();
        settings.setTerminalStartDirectory("/does/not/exist/chrono");

        LocalShellSpec spec = TerminalPanel.buildShellSpec(settings);

        assertEquals(Path.of(System.getProperty("user.home")), spec.directory());
    }

    @Test
    void testDefaultLook() {
        TerminalLook look = TerminalPanel.buildLook(new Settings());

        assertEquals(TerminalLook.dark().background(), look.background());
        assertEquals(TerminalLook.dark().fontFamily(), look.fontFamily());
        assertEquals(14, look.fontSize());
        assertEquals(5000, look.scrollback());
        assertFalse(look.copyOnSelect());
        assertTrue(look.ctrlCCopies());
        assertTrue(look.ctrlVPastes());
    }

    @Test
    void testCustomLook() {
        Settings settings = new Settings();
        settings.setTerminalTheme("LIGHT");
        settings.setTerminalFontFamily("Fira Code");
        settings.setTerminalFontSize(18);
        settings.setTerminalScrollback(200);
        settings.setTerminalCopyOnSelect(true);
        settings.setTerminalCtrlCCopies(false);
        settings.setTerminalCtrlVPastes(false);

        TerminalLook look = TerminalPanel.buildLook(settings);

        assertEquals(TerminalLook.light().background(), look.background());
        assertEquals("'Fira Code', monospace", look.fontFamily());
        assertEquals(18, look.fontSize());
        assertEquals(200, look.scrollback());
        assertTrue(look.copyOnSelect());
        assertFalse(look.ctrlCCopies());
        assertFalse(look.ctrlVPastes());
    }
}
