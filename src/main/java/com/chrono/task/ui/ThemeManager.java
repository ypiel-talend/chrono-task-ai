package com.chrono.task.ui;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;

import java.util.Objects;

/**
 * Applies the application look: an AtlantaFX base theme (Primer dark/light) as user agent stylesheet,
 * plus {@code app.css} for the app-specific styling. Also builds the CSS of the markdown preview WebView,
 * which is not styled by JavaFX CSS.
 */
public final class ThemeManager {

    public static final String DARK = "DARK";
    public static final String LIGHT = "LIGHT";

    private static final String APP_CSS = Objects.requireNonNull(
            ThemeManager.class.getResource("/com/chrono/task/view/app.css")).toExternalForm();

    private static boolean dark = true;

    private ThemeManager() {
    }

    /** {@code true} unless the setting is explicitly "LIGHT" (null/unknown values fall back to dark). */
    public static boolean isDark(String uiTheme) {
        return !LIGHT.equalsIgnoreCase(uiTheme == null ? "" : uiTheme.trim());
    }

    /** Whether the theme currently applied is dark. */
    public static boolean isDark() {
        return dark;
    }

    /** Switch the global theme (affects every window, dialogs included) and style the given scene. */
    public static void apply(Scene scene, String uiTheme) {
        dark = isDark(uiTheme);
        Application.setUserAgentStylesheet(dark
                ? new PrimerDark().getUserAgentStylesheet()
                : new PrimerLight().getUserAgentStylesheet());
        if (scene != null) {
            style(scene.getRoot());
        }
    }

    /** Add the app stylesheet to a dialog (its own scene does not inherit the main scene's stylesheets). */
    public static void style(Dialog<?> dialog) {
        style(dialog.getDialogPane());
    }

    private static void style(Parent root) {
        if (!root.getStylesheets().contains(APP_CSS)) {
            root.getStylesheets().add(APP_CSS);
        }
        root.getStyleClass().removeAll("theme-dark", "theme-light");
        root.getStyleClass().add(dark ? "theme-dark" : "theme-light");
    }

    /** CSS of the markdown preview page. {@code fontFamily} is a CSS font-family value. */
    public static String markdownCss(boolean dark, String fontFamily) {
        String bg = dark ? "#0d1117" : "#ffffff";
        String fg = dark ? "#c9d1d9" : "#1f2328";
        String muted = dark ? "#8b949e" : "#656d76";
        String border = dark ? "#30363d" : "#d0d7de";
        String codeBg = dark ? "#161b22" : "#f6f8fa";
        String link = dark ? "#58a6ff" : "#0969da";
        String accentSubtle = dark ? "rgba(56,139,253,0.15)" : "#ddf4ff";
        return "html { background: " + bg + "; }"
                + "body { font-family: " + fontFamily + "; font-size: 14px; line-height: 1.6; color: " + fg
                + "; background: " + bg + "; padding: 8px 24px 24px 24px; margin: 0; }"
                + "a { color: " + link + "; text-decoration: none; } a:hover { text-decoration: underline; }"
                + "h1, h2, h3 { border-bottom: 1px solid " + border + "; padding-bottom: 6px; margin-top: 24px; font-weight: 600; }"
                + "h1 { font-size: 1.6em; } h2 { font-size: 1.35em; } h3 { font-size: 1.15em; }"
                + "code { font-family: 'JetBrains Mono', 'Fira Code', monospace; font-size: 0.9em; background-color: " + codeBg
                + "; border: 1px solid " + border + "; padding: 1px 5px; border-radius: 6px; }"
                + "pre { background-color: " + codeBg + "; border: 1px solid " + border
                + "; padding: 12px 14px; border-radius: 8px; overflow-x: auto; }"
                + "pre code { border: none; padding: 0; background: transparent; }"
                + "blockquote { margin: 0; border-left: 4px solid " + link + "; background: " + accentSubtle
                + "; padding: 6px 14px; color: " + fg + "; border-radius: 0 6px 6px 0; }"
                + "table { border-collapse: collapse; } th, td { border: 1px solid " + border + "; padding: 6px 12px; }"
                + "th { background: " + codeBg + "; }"
                + "hr { border: none; border-top: 1px solid " + border + "; margin: 16px 0; }"
                + "::-webkit-scrollbar { width: 10px; height: 10px; }"
                + "::-webkit-scrollbar-thumb { background: " + border + "; border-radius: 5px; }"
                + "::-webkit-scrollbar-track { background: transparent; }"
                + ".edit-icon { cursor: pointer; color: " + link + "; font-size: 1.1em; margin-right: 8px;"
                + " padding: 0 4px; border-radius: 4px; }"
                + ".edit-icon:hover { background: " + accentSubtle + "; }"
                + ".daily-date { color: " + muted + "; font-weight: 600; }";
    }
}
