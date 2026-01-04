package com.chrono.task.service;

import com.chrono.task.model.ScheduledReminder;
import com.chrono.task.model.SchedulingData;
import com.chrono.task.persistence.SchedulingStorageService;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SchedulingService {

    private final SchedulingStorageService storageService;
    private final NotificationService notificationService;
    private final ScheduledExecutorService scheduler;
    private SchedulingData schedulingData;

    public SchedulingService(SchedulingStorageService storageService, NotificationService notificationService) {
        this.storageService = storageService;
        this.notificationService = notificationService;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void init() {
        this.schedulingData = storageService.load();
        startScheduler();
    }

    private void startScheduler() {
        // Check every minute for reminders that need to be triggered
        scheduler.scheduleAtFixedRate(this::checkReminders, 0, 1, TimeUnit.MINUTES);
    }

    private void checkReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalTime currentTime = now.toLocalTime().truncatedTo(ChronoUnit.MINUTES);

        for (ScheduledReminder reminder : schedulingData.getReminders()) {
            if (!reminder.isEnabled()) {
                continue;
            }

            boolean shouldTrigger = false;

            if (reminder.getType() == ScheduledReminder.ReminderType.ONE_TIME) {
                if (reminder.getDate().equals(today) && reminder.getTime().truncatedTo(ChronoUnit.MINUTES).equals(currentTime)) {
                    shouldTrigger = true;
                }
            } else if (reminder.getType() == ScheduledReminder.ReminderType.WEEKLY) {
                DayOfWeek todayDay = today.getDayOfWeek();
                if (reminder.getDayOfWeek().equals(todayDay) &&
                    reminder.getWeeklyTime().truncatedTo(ChronoUnit.MINUTES).equals(currentTime)) {
                    shouldTrigger = true;
                }
            }

            if (shouldTrigger) {
                triggerReminder(reminder);
            }
        }
    }

    private void triggerReminder(ScheduledReminder reminder) {
        log.info("Triggering reminder: {}", reminder.getTitle());

        String message = reminder.getDescription();
        if (!reminder.getLink().isEmpty()) {
            message += "\n" + reminder.getLink();
        }

        notificationService.sendNotification(
            "Reminder: " + reminder.getTitle(),
            message,
            TrayIcon.MessageType.INFO
        );
    }

    public void addReminder(ScheduledReminder reminder) {
        schedulingData.getReminders().add(reminder);
        save();
    }

    public void deleteReminder(ScheduledReminder reminder) {
        schedulingData.getReminders().remove(reminder);
        save();
    }

    public void updateReminder(ScheduledReminder reminder) {
        save();
    }

    public List<ScheduledReminder> getReminders() {
        return schedulingData.getReminders();
    }

    private void save() {
        storageService.save(schedulingData);
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}