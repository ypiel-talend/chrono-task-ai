package com.chrono.task.service;

import com.chrono.task.model.DataStore;
import com.chrono.task.model.Task;
import com.chrono.task.persistence.StorageService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TaskService {

    private final StorageService storageService;
    private final ObservableList<Task> tasks;
    private final ScheduledExecutorService autoSaveScheduler;

    public TaskService(StorageService storageService) {
        this.storageService = storageService;
        this.tasks = FXCollections.observableArrayList();
        this.autoSaveScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AutoSave-Thread");
            t.setDaemon(true);
            return t;
        });
    }

    public void init() throws IOException {
        DataStore data = storageService.load();
        if (data.getTasks() != null) {
            tasks.setAll(data.getTasks());
            // Ensure they are sorted by order
            tasks.sort(Comparator.comparingInt(Task::getOrder));
        }
        
        // Schedule auto-save every 30 seconds
        autoSaveScheduler.scheduleAtFixedRate(this::saveSafely, 30, 30, TimeUnit.SECONDS);
    }

    public ObservableList<Task> getTasks() {
        return tasks;
    }

    public Task createTask(String description) {
        Task task = Task.builder()
                .description(description)
                .order(tasks.size()) // Append to end
                .build();
        tasks.add(task);
        return task;
    }

    /**
     * Filters tasks based on a query string against description, jiraUrl, or tags.
     */
    public List<Task> filter(String query) {
        if (query == null || query.isBlank()) {
            return tasks;
        }
        String lowerQuery = query.toLowerCase();
        return tasks.stream()
                .filter(t -> (t.getDescription() != null && t.getDescription().toLowerCase().contains(lowerQuery)) ||
                             (t.getJiraUrl() != null && t.getJiraUrl().toLowerCase().contains(lowerQuery)) ||
                             (t.getTags() != null && t.getTags().stream().anyMatch(tag -> tag.toLowerCase().contains(lowerQuery))))
                .collect(Collectors.toList());
    }

    public void updateOrder(List<Task> newOrder) {
        for (int i = 0; i < newOrder.size(); i++) {
            newOrder.get(i).setOrder(i);
        }
        tasks.setAll(newOrder); // Update observable list
    }

    public void saveSafely() {
        try {
            // Create a snapshot to save
            DataStore store = new DataStore(List.copyOf(tasks));
            storageService.save(store);
            System.out.println("Auto-saved at " + java.time.LocalDateTime.now());
        } catch (IOException e) {
            e.printStackTrace(); // Log error (simple stdout for now)
        }
    }
    
    public void shutdown() {
        autoSaveScheduler.shutdown();
        saveSafely(); // Force save on exit
    }
}
