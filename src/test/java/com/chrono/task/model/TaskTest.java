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
    void testAddTime() {
        Task task = new Task();
        LocalDate now = LocalDate.now();
        task.addTime(now, Duration.ofMinutes(10));

        assertEquals(10, task.getTimeForDate(now).toMinutes());
        assertEquals(10, task.getTotalTime().toMinutes());

        task.addTime(now, Duration.ofMinutes(5));
        assertEquals(15, task.getTimeForDate(now).toMinutes());
    }

    @Test
    void testTotalTimeMultiDays() {
        Task task = new Task();
        LocalDate d1 = LocalDate.of(2023, 1, 1);
        LocalDate d2 = LocalDate.of(2023, 1, 2);

        task.addTime(d1, Duration.ofHours(1));
        task.addTime(d2, Duration.ofHours(2));

        assertEquals(Duration.ofHours(3), task.getTotalTime());
    }

    @Test
    void testDurationStats() {
        Task task = new Task();
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate old = today.minusDays(31);

        task.addTime(today, Duration.ofMinutes(10));
        task.addTime(yesterday, Duration.ofMinutes(20));
        task.addTime(old, Duration.ofMinutes(100));

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
}
