package com.chrono.task.ui;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GNOME Shell never uses a window's own icon (_NET_WM_ICON) in the dash, taskbar or Alt-Tab: it only shows the
 * icon of the {@code .desktop} file whose {@code StartupWMClass} matches the window class, else a generic icon.
 * So on GNOME the app registers itself: icon + desktop entry in {@code ~/.local/share}, refreshed at each start
 * (the entry also works as a launcher, with the exact command line of the running JVM).
 */
@Slf4j
public final class DesktopIntegration {

    static final String WM_CLASS = "com.chrono.task.ChronoApp";
    private static final String FILE_NAME = "chrono-task-ai";

    private DesktopIntegration() {
    }

    public static void registerIfGnome() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("linux")
                || !System.getenv().getOrDefault("XDG_CURRENT_DESKTOP", "").toUpperCase().contains("GNOME")
                // Test runs with a throwaway settings file must not overwrite the user's launcher
                || System.getProperty("chronotaskai.settings.file") != null)
            return;
        try {
            String xdgDataHome = System.getenv("XDG_DATA_HOME");
            Path dataHome = xdgDataHome != null && !xdgDataHome.isBlank()
                    ? Path.of(xdgDataHome)
                    : Path.of(System.getProperty("user.home"), ".local", "share");

            Path icon = dataHome.resolve("icons").resolve(FILE_NAME + ".png");
            try (InputStream in = DesktopIntegration.class.getResourceAsStream("/com/chrono/task/icons/app-icon-512.png")) {
                if (in == null)
                    return;
                Files.createDirectories(icon.getParent());
                Files.copy(in, icon, StandardCopyOption.REPLACE_EXISTING);
            }

            // Not ProcessHandle.Info: it drops the arguments when the command line exceeds 4 KB (long module path)
            List<String> command = new ArrayList<>(
                    List.of(new String(Files.readAllBytes(Path.of("/proc/self/cmdline"))).split("\0")));
            if (command.isEmpty())
                return;
            // argv[0] may be a bare "java": launchers do not have the shell PATH (sdkman)
            command.set(0, Path.of(System.getProperty("java.home"), "bin", "java").toString());

            String entry = buildDesktopEntry(command, System.getProperty("user.dir"), icon.toString());
            Path desktopFile = dataHome.resolve("applications").resolve(FILE_NAME + ".desktop");
            if (Files.exists(desktopFile) && Files.readString(desktopFile).equals(entry))
                return;
            Files.createDirectories(desktopFile.getParent());
            Files.writeString(desktopFile, entry);
            log.info("Registered desktop entry {}", desktopFile);
        } catch (IOException | RuntimeException e) {
            log.warn("Could not register the desktop entry: {}", e.getMessage());
        }
    }

    static String buildDesktopEntry(List<String> command, String workDir, String iconPath) {
        return """
                [Desktop Entry]
                Type=Application
                Name=Chrono Task AI
                Comment=Daily tasks, time tracking and notes
                Icon=%s
                Exec=%s
                Path=%s
                Terminal=false
                Categories=Office;ProjectManagement;
                StartupWMClass=%s
                """.formatted(iconPath,
                command.stream().map(DesktopIntegration::quoteExecArg).collect(Collectors.joining(" ")),
                workDir, WM_CLASS);
    }

    /** Desktop Entry spec: quote each arg, escape {@code " ` $ \} inside quotes, and double {@code %}. */
    static String quoteExecArg(String arg) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : arg.toCharArray()) {
            switch (c) {
                case '"', '`', '$', '\\' -> sb.append('\\').append(c);
                case '%' -> sb.append("%%");
                default -> sb.append(c);
            }
        }
        // The desktop file itself is unescaped first: a literal backslash must be doubled once more
        return sb.append('"').toString().replace("\\", "\\\\");
    }
}
