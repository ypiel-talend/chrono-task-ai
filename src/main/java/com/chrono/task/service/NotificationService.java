package com.chrono.task.service;

import java.awt.*;
import java.awt.TrayIcon.MessageType;

public class NotificationService {

    private TrayIcon trayIcon;

    public NotificationService() {
        if (SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();

            // We need an image for the tray icon. For now, we'll use a blank transparent
            // image
            // or a placeholder if available. Java doesn't easily let us use app icons here
            // without a file path.
            Image image = Toolkit.getDefaultToolkit().createImage(new byte[0]);

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
}
