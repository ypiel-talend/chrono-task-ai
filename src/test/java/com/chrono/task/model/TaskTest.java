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
}
