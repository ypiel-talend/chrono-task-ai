package com.chrono.task.service;

import com.chrono.task.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TimerServiceTest {

    private TimerService timerService;

    @BeforeEach
    public void setup() {
        timerService = new TimerService();
    }

    @Test
    public void testPauseResume() {
        Task task = new Task();
        timerService.setActiveTask(task);
        assertFalse(timerService.isPaused(), "Should not be paused initially");

        timerService.pause();
        assertTrue(timerService.isPaused(), "Should be paused after pause()");

        timerService.resume();
        assertFalse(timerService.isPaused(), "Should not be paused after resume()");
    }

    @Test
    public void testTaskSwitchResetsPause() {
        Task task1 = new Task();
        timerService.setActiveTask(task1);
        timerService.pause();
        assertTrue(timerService.isPaused());

        Task task2 = new Task();
        timerService.setActiveTask(task2);
        assertFalse(timerService.isPaused(), "Switching task should reset pause state");
    }
}
