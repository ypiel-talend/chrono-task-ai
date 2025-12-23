package com.chrono.task.service;

import com.chrono.task.model.Settings;
import com.chrono.task.model.TaskStatus;
import javafx.application.Platform;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class JiraRefreshService {

    private final JiraService jiraService;
    private final TaskService taskService;
    private final Settings settings;
    private final javafx.beans.property.BooleanProperty isRefreshing = new javafx.beans.property.SimpleBooleanProperty(
            false);
    private ScheduledExecutorService scheduler;

    public JiraRefreshService(JiraService jiraService, TaskService taskService, Settings settings) {
        this.jiraService = jiraService;
        this.taskService = taskService;
        this.settings = settings;
    }

    public javafx.beans.property.ReadOnlyBooleanProperty isRefreshingProperty() {
        return isRefreshing;
    }

    public synchronized void start() {
        if (scheduler != null && !scheduler.isShutdown()) {
            return;
        }

        if (!settings.isJiraRefreshEnabled()) {
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "JiraRefresh-Thread");
            t.setDaemon(true);
            return t;
        });

        long interval = settings.getJiraRefreshInterval();
        if (interval < 1)
            interval = 1;

        TimeUnit timeUnit = switch (settings.getJiraRefreshUnit()) {
            case SECONDS -> TimeUnit.SECONDS;
            case MINUTES -> TimeUnit.MINUTES;
            case HOURS -> TimeUnit.HOURS;
            default -> TimeUnit.MINUTES;
        };

        scheduler.scheduleAtFixedRate(this::refreshAll, interval, interval, timeUnit);
        System.out
                .println("JiraRefreshService started with interval: " + interval + " " + settings.getJiraRefreshUnit());
    }

    public synchronized void stop() {
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
            System.out.println("JiraRefreshService stopped");
        }
    }

    public synchronized void restart() {
        stop();
        start();
    }

    private void refreshAll() {
        if (settings.getJiraEmail() == null || settings.getJiraEmail().isBlank() ||
                settings.getJiraApiToken() == null || settings.getJiraApiToken().isBlank()) {
            return;
        }

        Platform.runLater(() -> isRefreshing.set(true));

        java.util.List<java.util.concurrent.CompletableFuture<Void>> futures = new java.util.ArrayList<>();

        taskService.getTasks().forEach(task -> {
            if (task.getJiraUrl() != null && !task.getJiraUrl().isBlank() &&
                    task.getStatus() != TaskStatus.DONE && task.getStatus() != TaskStatus.NONE) {

                var future = jiraService
                        .fetchIssue(task.getJiraUrl(), settings.getJiraEmail(), settings.getJiraApiToken())
                        .thenAccept(issue -> {
                            TaskStatus newStatus = jiraService.mapStatus(issue.status);
                            if (newStatus != task.getStatus()) {
                                Platform.runLater(() -> {
                                    task.setStatus(newStatus);
                                });
                            }
                        })
                        .exceptionally(ex -> {
                            System.err.println(
                                    "Failed to refresh Jira task: " + task.getDescription() + " - " + ex.getMessage());
                            return null;
                        });
                futures.add(future);
            }
        });

        if (futures.isEmpty()) {
            Platform.runLater(() -> isRefreshing.set(false));
        } else {
            java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0]))
                    .whenComplete((v, ex) -> Platform.runLater(() -> isRefreshing.set(false)));
        }
    }
}
