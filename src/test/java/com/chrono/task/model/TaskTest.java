package com.chrono.task.model;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class TaskTest {

    @Test
    void testTaskDefaults() {
        Task task = Task.builder().build();
        assertNotNull(task.getId());
        assertEquals(TaskStatus.TODO, task.getStatus());
        assertTrue(task.getTags().isEmpty());
    }

    @Test
    void testSetTime() {
        Task task = new Task();
        LocalDate now = LocalDate.now();
        task.setTime(now, Duration.ofMinutes(10));

        assertEquals(10, task.getTimeForDate(now).toMinutes());
        assertEquals(10, task.getTotalTime().toMinutes());

        task.setTime(now, Duration.ofMinutes(5));
        assertEquals(5, task.getTimeForDate(now).toMinutes());
    }

    @Test
    void testTotalTimeMultiDays() {
        Task task = new Task();
        LocalDate d1 = LocalDate.of(2023, 1, 1);
        LocalDate d2 = LocalDate.of(2023, 1, 2);

        task.setTime(d1, Duration.ofHours(1));
        task.setTime(d2, Duration.ofHours(2));

        assertEquals(Duration.ofHours(3), task.getTotalTime());
    }

    @Test
    void testDurationStats() {
        Task task = new Task();
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate old = today.minusDays(31);

        task.setTime(today, Duration.ofMinutes(10));
        task.setTime(yesterday, Duration.ofMinutes(20));
        task.setTime(old, Duration.ofMinutes(100));

        assertEquals(Duration.ofMinutes(10), task.getDurationToday());
        // 10 + 20 = 30. old is 31 days ago, should be excluded.
        // wait, I said "last 30 days" inclusive of today?
        // logic was: !entry.getKey().isBefore(start) && !entry.getKey().isAfter(today)
        // start = today.minusDays(30)
        // if today is day 31, start is day 1.
        // older is day 0 (31 days ago). So yes, excluded.

        assertEquals(Duration.ofMinutes(30), task.getDurationLast30Days());
        assertEquals(Duration.ofMinutes(130), task.getTotalTime());
    }

    @Test
    void testCleanHistory() {
        Task task = new Task();
        LocalDate d1 = LocalDate.of(2023, 1, 1);
        LocalDate d2 = LocalDate.of(2023, 1, 2);
        LocalDate d3 = LocalDate.of(2023, 1, 3);
        LocalDate d4 = LocalDate.of(2023, 1, 4);

        // Case 1: Short duration, no note -> Should be removed
        task.setTime(d1, Duration.ofMinutes(1));

        // Case 2: Long duration, no note -> Should be kept
        task.setTime(d2, Duration.ofMinutes(3));

        // Case 3: Short duration, with note -> Should be kept
        task.setTime(d3, Duration.ofMinutes(1));
        task.setDailyNote(d3, "My note");

        // Case 4: Exactly 2 minutes, no note -> Should be kept (strictly less than 2
        // removed)
        task.setTime(d4, Duration.ofMinutes(2));

        task.cleanupHistory();

        assertEquals(Duration.ZERO, task.getTimeForDate(d1)); // Removed
        assertEquals(Duration.ofMinutes(3), task.getTimeForDate(d2)); // Kept
        assertEquals(Duration.ofMinutes(1), task.getTimeForDate(d3)); // Kept
        assertEquals(Duration.ofMinutes(2), task.getTimeForDate(d4)); // Kept
    }
}