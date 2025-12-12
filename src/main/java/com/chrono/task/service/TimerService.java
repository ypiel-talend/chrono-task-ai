package com.chrono.task.service;

import com.chrono.task.model.Task;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TimerService {

    private final ObjectProperty<Task> activeTask = new SimpleObjectProperty<>();
    private Instant startTime;
    private final ScheduledExecutorService ticker;
    
    // Allows UI to observe "current session duration" to display ephemeral seconds
    // Or we simply update the task model periodically
    
    public TimerService() {
        this.ticker = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Timer-Ticker");
            t.setDaemon(true);
            return t;
        });
        
        // Every second, update the active task's time history
        this.ticker.scheduleAtFixedRate(this::tick, 1, 1, TimeUnit.SECONDS);
    }

    public void setActiveTask(Task task) {
        if (activeTask.get() == task) {
            return; // Already active
        }

        // Stop previous
        if (activeTask.get() != null) {
            stopTimer();
        }

        // Start new
        activeTask.set(task);
        if (task != null) {
            startTimer();
        }
    }

    private void startTimer() {
        startTime = Instant.now();
    }

    private void stopTimer() {
        // Finalize time for the task being stopped
        tick(); 
        startTime = null;
    }

    private void tick() {
        Task current = activeTask.get();
        if (current != null && startTime != null) {
            Instant now = Instant.now();
            Duration sessionDuration = Duration.between(startTime, now);
            
            // We add this small session chunk to the task and reset startTime to now
            // This prevents losing large chunks if crash, and allows real-time update
            current.addTime(LocalDate.now(), sessionDuration);
            startTime = now;
        }
    }
    
    public ObjectProperty<Task> activeTaskProperty() {
        return activeTask;
    }
    
    public void shutdown() {
        ticker.shutdown();
        stopTimer();
    }
}