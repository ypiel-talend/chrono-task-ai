package com.chrono.task.ui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DesktopIntegrationTest {

    @Test
    void entryMatchesWindowClassAndUsesAbsoluteIcon() {
        String entry = DesktopIntegration.buildDesktopEntry(
                List.of("/opt/jdk/bin/java", "-m", "com.chrono.task/com.chrono.task.ChronoApp"),
                "/home/me/app", "/home/me/.local/share/icons/chrono-task-ai.png");

        assertTrue(entry.startsWith("[Desktop Entry]\n"));
        assertTrue(entry.contains("\nStartupWMClass=com.chrono.task.ChronoApp\n"));
        assertTrue(entry.contains("\nIcon=/home/me/.local/share/icons/chrono-task-ai.png\n"));
        assertTrue(entry.contains("\nPath=/home/me/app\n"));
        assertTrue(entry.contains("\nExec=\"/opt/jdk/bin/java\" \"-m\" \"com.chrono.task/com.chrono.task.ChronoApp\"\n"));
    }

    @Test
    void execArgsAreQuotedAndEscaped() {
        assertEquals("\"a b\"", DesktopIntegration.quoteExecArg("a b"));
        assertEquals("\"\\\\$HOME\"", DesktopIntegration.quoteExecArg("$HOME"));
        assertEquals("\"100%%\"", DesktopIntegration.quoteExecArg("100%"));
        assertEquals("\"say \\\\\"hi\\\\\"\"", DesktopIntegration.quoteExecArg("say \"hi\""));
        assertEquals("\"C:\\\\\\\\x\"", DesktopIntegration.quoteExecArg("C:\\x"));
    }
}
