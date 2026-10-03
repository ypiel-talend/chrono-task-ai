package com.chrono.task.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import com.chrono.task.model.Settings;
import com.kodedu.terminalfx.TerminalExit;
import com.kodedu.terminalfx.TerminalLook;
import com.kodedu.terminalfx.TerminalSession;
import com.kodedu.terminalfx.TerminalView;
import com.kodedu.terminalfx.local.LocalShell;
import com.kodedu.terminalfx.local.LocalShellSpec;

import lombok.extern.slf4j.Slf4j;

/**
 * Bottom panel holding several local shell terminals, one per tab (toggled with Ctrl+F12).
 */
@Slf4j
public class TerminalPanel extends BorderPane {

    private final Settings settings;
    private final Runnable onHide;
    private final TabPane tabPane = new TabPane();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private int tabCounter = 0;

    public TerminalPanel(Settings settings, Runnable onHide) {
        this.settings = settings;
        this.onHide = onHide;

        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);

        Button newTabButton = new Button("+");
        newTabButton.setFocusTraversable(false);
        newTabButton.setOnAction(_ -> newTab());
        Button hideButton = new Button("✕");
        hideButton.setFocusTraversable(false);
        hideButton.setOnAction(_ -> onHide.run());

        Label title = new Label("Terminal (Ctrl+F12)");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(5, title, spacer, newTabButton, hideButton);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(2, 5, 2, 5));

        setTop(toolbar);
        setCenter(tabPane);

        // Last tab closed (by the user or by "exit"): hide the panel.
        tabPane.getTabs().addListener((javafx.collections.ListChangeListener<Tab>) _ -> {
            if (tabPane.getTabs().isEmpty()) {
                onHide.run();
            }
        });
    }

    /** Opens a terminal if none is open yet, and gives focus to the selected one. */
    public void ensureOneTab() {
        if (tabPane.getTabs().isEmpty()) {
            newTab();
        } else {
            focusSelected();
        }
    }

    public void newTab() {
        TerminalView view = new TerminalView(buildLook(settings));
        view.keysClaimedBy((key, ctrl, _, _, _) -> ctrl && "F12".equals(key));
        view.onCopy(text -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(text);
            Clipboard.getSystemClipboard().setContent(content);
        });
        view.onPasteRequested(() -> {
            String text = Clipboard.getSystemClipboard().getString();
            if (text != null) {
                view.paste(text);
            }
        });

        Tab tab = new Tab("Terminal " + (++tabCounter), view);
        view.onTitle(tab::setText);

        LocalShellSpec spec = buildShellSpec(settings);
        TerminalSession session = new TerminalSession(view, new LocalShell(spec, executor), executor);
        tab.setUserData(session);
        tab.setOnClosed(_ -> session.close());
        session.onExit(exit -> {
            if (exit instanceof TerminalExit.Failed failed) {
                log.error("Terminal failed: {}", Arrays.toString(spec.command()), failed.cause());
                view.write("\r\n[Terminal failed to start: " + failed.cause().getMessage() + "]\r\n");
            } else {
                // Shell ended ("exit") or tab closed: remove the tab if still there.
                tabPane.getTabs().remove(tab);
            }
        });
        session.start();

        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
        view.whenReady(() -> Platform.runLater(view::focusTerminal));
    }

    public void focusSelected() {
        Tab tab = tabPane.getSelectionModel().getSelectedItem();
        if (tab != null && tab.getContent() instanceof TerminalView view) {
            view.focusTerminal();
        }
    }

    /** Re-applies the look settings to the open terminals; shell settings only apply to new tabs. */
    public void applySettings() {
        TerminalLook look = buildLook(settings);
        tabPane.getTabs().forEach(tab -> {
            if (tab.getContent() instanceof TerminalView view) {
                view.look(look);
            }
        });
    }

    /** Closes all the shells. To be called when the application stops. */
    public void closeAll() {
        tabPane.getTabs().forEach(tab -> {
            if (tab.getUserData() instanceof TerminalSession session) {
                session.close();
            }
        });
        executor.shutdown();
    }

    static TerminalLook buildLook(Settings settings) {
        TerminalLook look = "LIGHT".equalsIgnoreCase(settings.getTerminalTheme())
                ? TerminalLook.light()
                : TerminalLook.dark();
        String fontFamily = settings.getTerminalFontFamily();
        if (fontFamily != null && !fontFamily.isBlank()) {
            look = look.withFontFamily("'" + fontFamily.replace("'", "") + "', monospace");
        }
        if (settings.getTerminalFontSize() > 0) {
            look = look.withFontSize(settings.getTerminalFontSize());
        }
        if (settings.getTerminalScrollback() > 0) {
            look = look.withScrollback(settings.getTerminalScrollback());
        }
        return look
                .withCopyOnSelect(settings.isTerminalCopyOnSelect())
                .withCtrlCCopies(settings.isTerminalCtrlCCopies())
                .withCtrlVPastes(settings.isTerminalCtrlVPastes());
    }

    static LocalShellSpec buildShellSpec(Settings settings) {
        LocalShellSpec spec = LocalShellSpec.defaultShell();
        String shell = settings.getTerminalShell();
        if (shell != null && !shell.isBlank()) {
            spec = spec.running(List.of(shell.trim().split("\\s+")));
        }
        String directory = settings.getTerminalStartDirectory();
        if (directory != null && !directory.isBlank()) {
            Path path = Path.of(directory.trim());
            if (Files.isDirectory(path)) {
                spec = spec.in(path);
            } else {
                log.warn("Terminal start directory '{}' does not exist, using the home directory", directory);
            }
        }
        return spec;
    }
}
