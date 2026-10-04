package com.chrono.task.controller;

import com.chrono.task.model.Task;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class MainControllerWindowTitleTest {

    private static final Duration TODAY = Duration.ofMinutes(83).plusSeconds(45);

    @Test
    void testNoActiveTaskShowsDayTotal() {
        assertEquals("Chrono Task AI · Day 05h12m",
                MainController.buildWindowTitle(null, false, Duration.ZERO, Duration.ofMinutes(312)));
    }

    @Test
    void testRunningTask() {
        Task task = Task.builder().description("Write docs").build();
        assertEquals("▶ 01:23 · Write docs — Chrono Task AI",
                MainController.buildWindowTitle(task, false, TODAY, Duration.ZERO));
    }

    @Test
    void testRunningJiraTaskShowsKey() {
        Task task = Task.builder().description("Fix login")
                .jiraUrl("https://acme.atlassian.net/browse/PROJ-42").isJira(true).build();
        assertEquals("▶ 01:23 · PROJ-42: Fix login — Chrono Task AI",
                MainController.buildWindowTitle(task, false, TODAY, Duration.ZERO));
    }

    @Test
    void testPausedTask() {
        Task task = Task.builder().description("Write docs").build();
        assertEquals("⏸ Paused · Write docs — Chrono Task AI",
                MainController.buildWindowTitle(task, true, TODAY, Duration.ZERO));
    }

    @Test
    void testLongDurations() {
        Task task = Task.builder().description("Marathon").build();
        assertEquals("▶ 123:04 · Marathon — Chrono Task AI",
                MainController.buildWindowTitle(task, false, Duration.ofHours(123).plusMinutes(4), Duration.ZERO));
        assertEquals("Chrono Task AI · Day 10h00m",
                MainController.buildWindowTitle(null, false, Duration.ZERO, Duration.ofHours(10)));
    }
}
