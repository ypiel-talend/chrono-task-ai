package com.chrono.task.service;

import java.awt.*;
import java.awt.TrayIcon.MessageType;

public class NotificationService {

    private TrayIcon trayIcon;

    public NotificationService() {
        if (SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();

            var iconUrl = NotificationService.class.getResource("/com/chrono/task/icons/app-icon-32.png");
            Image image = iconUrl != null
                    ? Toolkit.getDefaultToolkit().createImage(iconUrl)
                    : Toolkit.getDefaultToolkit().createImage(new byte[0]);

            this.trayIcon = new TrayIcon(image, "Chrono Task AI");
            this.trayIcon.setImageAutoSize(true);
            try {
                tray.add(this.trayIcon);
            } catch (AWTException e) {
                System.err.println("TrayIcon could not be added.");
            }
        }
    }

    public void sendNotification(String title, String message, MessageType type) {
        if (trayIcon != null) {
            trayIcon.displayMessage(title, message, type);
        } else {
            // Fallback to console or potentially a JavaFX dialog if needed
            System.err.println("Notification: [" + title + "] " + message);
        }
    }

    public void shutdown() {
        if (trayIcon != null && SystemTray.isSupported()) {
            SystemTray.getSystemTray().remove(trayIcon);
        }
    }
}
