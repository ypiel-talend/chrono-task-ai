package com.chrono.task.service;

import com.chrono.task.model.Settings;
import com.chrono.task.model.TaskStatus;
import javafx.application.Platform;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

@Slf4j
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

        // First, refresh existing tasks with Jira URLs
        taskService.getTasks().forEach(task -> {
            if (task.getJiraUrl() != null && !task.getJiraUrl().isBlank() &&
                    task.getStatus() != TaskStatus.DONE && task.getStatus() != TaskStatus.NONE) {

                var future = jiraService
                        .fetchIssue(task.getJiraUrl(), settings.getJiraEmail(), settings.getJiraApiToken())
                        .thenAccept(issue -> {
                            TaskStatus newStatus = jiraService.mapStatus(issue.status());
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

        // Second, fetch new tasks from JQL query if configured
        if (settings.getJqlQuery() != null && !settings.getJqlQuery().isBlank() &&
                settings.getJiraBaseUrl() != null && !settings.getJiraBaseUrl().isBlank()) {

            var jqlFuture = jiraService
                    .searchByJql(settings.getJqlQuery(), settings.getJiraEmail(), settings.getJiraApiToken(), settings.getJiraBaseUrl())
                    .thenAccept(issues -> {
                        Platform.runLater(() -> {
                            createTasksFromJqlResults(issues, settings.getJiraBaseUrl());
                        });
                    })
                    .exceptionally(ex -> {
                        System.err.println("Failed to execute JQL query: " + ex.getMessage());
                        return null;
                    });
            futures.add(jqlFuture);
        }

        if (futures.isEmpty()) {
            Platform.runLater(() -> isRefreshing.set(false));
        } else {
            java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0]))
                    .whenComplete((v, ex) -> Platform.runLater(() -> isRefreshing.set(false)));
        }
    }

    private void createTasksFromJqlResults(java.util.List<JiraService.JiraIssue> issues, String baseUrl) {
        java.util.Set<String> excludedStatuses = java.util.Set.of("rejected", "closed", "done", "final check", "eap");

        log.info("JQL found %s issues".formatted(issues.size()));
        for (JiraService.JiraIssue issue : issues) {
            // Skip if status is in excluded list
            if (issue.status() != null && excludedStatuses.contains(issue.status().toLowerCase())) {
                continue;
            }

            String jiraUrl = jiraService.buildIssueUrl(baseUrl, issue.key());

            // Check if task already exists with this Jira URL
            boolean taskExists = taskService.getTasks().stream()
                    .anyMatch(t -> jiraUrl.equals(t.getJiraUrl()));

            if (!taskExists) {
                // Create new task
                try {
                    com.chrono.task.model.Task newTask = com.chrono.task.model.Task.builder()
                            .description(issue.summary())
                            .jiraUrl(jiraUrl)
                            .isJira(true)
                            .status(jiraService.mapStatus(issue.status()))
                            .tag("new_auto")
                            .order(0) // Will be inserted at the top
                            .build();

                    // Insert at the beginning of the list
                    taskService.getTasks().add(0, newTask);

                    // Update order for all tasks
                    for (int i = 0; i < taskService.getTasks().size(); i++) {
                        taskService.getTasks().get(i).setOrder(i);
                    }

                    System.out.println("Auto-created task from JQL: " + issue.key() + " - " + issue.summary());
                } catch (Exception e) {
                    System.err.println("Failed to create task for Jira issue " + issue.key() + ": " + e.getMessage());
                }
            }
        }
    }
}