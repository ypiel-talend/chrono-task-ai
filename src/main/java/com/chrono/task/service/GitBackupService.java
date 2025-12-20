package com.chrono.task.service;

import com.chrono.task.model.Settings;
import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GitBackupService {

    private final GitService gitService;
    private final Settings settings;
    private final NotificationService notificationService;
    private ScheduledExecutorService scheduler;

    public GitBackupService(GitService gitService, Settings settings, NotificationService notificationService) {
        this.gitService = gitService;
        this.settings = settings;
        this.notificationService = notificationService;
    }

    public synchronized void restart() {
        stop();
        start();
    }

    public synchronized void start() {
        if (!settings.isGitBackupEnabled()) {
            stop();
            return;
        }

        if (!gitService.isGitInstalled()) {
            notificationService.sendNotification(
                    "Git Backup Error",
                    "Git is not installed. Automated backups are disabled.",
                    java.awt.TrayIcon.MessageType.ERROR);
            stop();
            return;
        }

        if (scheduler != null && !scheduler.isShutdown()) {
            return; // Already running
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "GitBackupThread");
            t.setDaemon(true);
            return t;
        });

        long interval = settings.getGitBackupInterval();
        TimeUnit unit = switch (settings.getGitBackupUnit()) {
            case DAYS -> TimeUnit.DAYS;
            case HOURS -> TimeUnit.HOURS;
            case MINUTES -> TimeUnit.MINUTES;
            default -> TimeUnit.HOURS;
        };

        scheduler.scheduleAtFixedRate(this::performBackup, interval, interval, unit);

        // Initial init if needed
        try {
            gitService.initRepository(new File(settings.getDataStoragePath()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void stop() {
        if (settings.isGitBackupEnabled() && gitService.isGitInstalled()) {
            performBackup();
        }

        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
            scheduler = null;
        }
    }

    private void performBackup() {
        if (!settings.isGitBackupEnabled()) {
            return;
        }
        try {
            File path = new File(settings.getDataStoragePath());
            gitService.backup(path, "data.json");
        } catch (Exception e) {
            System.err.println("Git backup failed: " + e.getMessage());
        }
    }
}
