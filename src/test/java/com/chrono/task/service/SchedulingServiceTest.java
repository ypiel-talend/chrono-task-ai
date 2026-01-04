package com.chrono.task.service;

import com.chrono.task.model.ScheduledReminder;
import com.chrono.task.model.SchedulingData;
import com.chrono.task.persistence.SchedulingStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class SchedulingServiceTest {

    @TempDir
    File tempDir;

    private SchedulingService schedulingService;
    private SchedulingStorageService storageService;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        String testFilePath = new File(tempDir, "test-scheduling.json").getAbsolutePath();
        storageService = new SchedulingStorageService(testFilePath);
        notificationService = new NotificationService();
        schedulingService = new SchedulingService(storageService, notificationService);
        schedulingService.init();
    }

    @Test
    void testAddOneTimeReminder() {
        ScheduledReminder reminder = ScheduledReminder.builder()
                .title("Test Reminder")
                .description("Test Description")
                .link("https://example.com")
                .type(ScheduledReminder.ReminderType.ONE_TIME)
                .date(LocalDate.now().plusDays(1))
                .time(LocalTime.of(14, 30))
                .enabled(true)
                .build();

        schedulingService.addReminder(reminder);

        assertEquals(1, schedulingService.getReminders().size());
        ScheduledReminder saved = schedulingService.getReminders().get(0);
        assertEquals("Test Reminder", saved.getTitle());
        assertEquals("Test Description", saved.getDescription());
        assertEquals("https://example.com", saved.getLink());
        assertEquals(ScheduledReminder.ReminderType.ONE_TIME, saved.getType());
    }

    @Test
    void testAddWeeklyReminder() {
        ScheduledReminder reminder = ScheduledReminder.builder()
                .title("Weekly Meeting")
                .description("Team standup")
                .type(ScheduledReminder.ReminderType.WEEKLY)
                .dayOfWeek(DayOfWeek.MONDAY)
                .weeklyTime(LocalTime.of(9, 0))
                .enabled(true)
                .build();

        schedulingService.addReminder(reminder);

        assertEquals(1, schedulingService.getReminders().size());
        ScheduledReminder saved = schedulingService.getReminders().get(0);
        assertEquals("Weekly Meeting", saved.getTitle());
        assertEquals(ScheduledReminder.ReminderType.WEEKLY, saved.getType());
        assertEquals(DayOfWeek.MONDAY, saved.getDayOfWeek());
        assertEquals(LocalTime.of(9, 0), saved.getWeeklyTime());
    }

    @Test
    void testDeleteReminder() {
        ScheduledReminder reminder = ScheduledReminder.builder()
                .title("To Delete")
                .type(ScheduledReminder.ReminderType.ONE_TIME)
                .date(LocalDate.now())
                .time(LocalTime.now())
                .build();

        schedulingService.addReminder(reminder);
        assertEquals(1, schedulingService.getReminders().size());

        schedulingService.deleteReminder(reminder);
        assertEquals(0, schedulingService.getReminders().size());
    }

    @Test
    void testUpdateReminder() {
        ScheduledReminder reminder = ScheduledReminder.builder()
                .title("Original Title")
                .type(ScheduledReminder.ReminderType.ONE_TIME)
                .date(LocalDate.now())
                .time(LocalTime.now())
                .enabled(true)
                .build();

        schedulingService.addReminder(reminder);

        reminder.setTitle("Updated Title");
        reminder.setEnabled(false);
        schedulingService.updateReminder(reminder);

        // Reload from storage to verify persistence
        SchedulingData reloaded = storageService.load();
        assertEquals(1, reloaded.getReminders().size());
        ScheduledReminder updated = reloaded.getReminders().get(0);
        assertEquals("Updated Title", updated.getTitle());
        assertFalse(updated.isEnabled());
    }

    @Test
    void testPersistence() {
        ScheduledReminder reminder1 = ScheduledReminder.builder()
                .title("Reminder 1")
                .type(ScheduledReminder.ReminderType.ONE_TIME)
                .date(LocalDate.now())
                .time(LocalTime.now())
                .build();

        ScheduledReminder reminder2 = ScheduledReminder.builder()
                .title("Reminder 2")
                .type(ScheduledReminder.ReminderType.WEEKLY)
                .dayOfWeek(DayOfWeek.FRIDAY)
                .weeklyTime(LocalTime.of(17, 0))
                .build();

        schedulingService.addReminder(reminder1);
        schedulingService.addReminder(reminder2);

        // Create new service instance to test persistence
        String testFilePath = new File(tempDir, "test-scheduling.json").getAbsolutePath();
        SchedulingStorageService newStorageService = new SchedulingStorageService(testFilePath);
        SchedulingData loaded = newStorageService.load();

        assertEquals(2, loaded.getReminders().size());
    }
}