package com.chrono.task.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Task {

    @Builder.Default
    private String id = UUID.randomUUID().toString();

    private int order;

    private String description;

    private String jiraUrl;

    private boolean isJira;

    @Builder.Default
    private TaskStatus status = TaskStatus.TODO;

    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Builder.Default
    private Map<LocalDate, TaskDailyWork> taskHistory = new HashMap<>();

    @Builder.Default
    private String markdownContent = "";

    /**
     * Helper to add time to a specific date.
     */
    public void addTime(LocalDate date, Duration duration) {
        taskHistory.compute(date, (d, work) -> {
            if (work == null) {
                return TaskDailyWork.builder().duration(duration).build();
            }
            work.setDuration(work.getDuration().plus(duration));
            return work;
        });
    }

    public void setTime(LocalDate date, Duration duration) {
        taskHistory.compute(date, (d, work) -> {
            if (work == null) {
                return TaskDailyWork.builder().duration(duration).build();
            }
            work.setDuration(duration);
            return work;
        });
    }

    @JsonIgnore
    public Duration getTotalTime() {
        return taskHistory.values().stream()
                .map(TaskDailyWork::getDuration)
                .reduce(Duration.ZERO, Duration::plus);
    }

    public Duration getTimeForDate(LocalDate date) {
        TaskDailyWork work = taskHistory.get(date);
        return work != null ? work.getDuration() : Duration.ZERO;
    }

    @JsonIgnore
    public Duration getDurationLast30Days() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(30);
        return taskHistory.entrySet().stream()
                .filter(entry -> !entry.getKey().isBefore(start) && !entry.getKey().isAfter(today))
                .map(entry -> entry.getValue().getDuration())
                .reduce(Duration.ZERO, Duration::plus);
    }

    @JsonIgnore
    public Duration getDurationToday() {
        return getTimeForDate(LocalDate.now());
    }

    public String getDailyNote(LocalDate date) {
        TaskDailyWork work = taskHistory.get(date);
        return work != null ? work.getNote() : "";
    }

    public void setDailyNote(LocalDate date, String note) {
        taskHistory.compute(date, (d, work) -> {
            if (work == null) {
                return TaskDailyWork.builder().note(note).build();
            }
            work.setNote(note);
            return work;
        });
    }

    @JsonIgnore
    public String getHistoryLabel() {
        StringBuilder sb = new StringBuilder();
        if (this.getJiraUrl() != null && !this.getJiraUrl().isBlank()) {
            sb.append(this.getJiraUrl());
            sb.append(": ");
        }
        sb.append(this.getDescription());
        return sb.toString();
    }

    @JsonIgnore
    public String getLabel() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getOrder()).append(" - ");
        if (isJira) {
            sb.append(this.getJiraUrl().substring(this.getJiraUrl().lastIndexOf('/') + 1));
            sb.append(": ");
        }
        sb.append(this.getDescription());
        return sb.toString();
    }

    public void cleanupHistory() {
        taskHistory.entrySet().removeIf(entry -> {
            TaskDailyWork work = entry.getValue();
            boolean isShort = work.getDuration().compareTo(Duration.ofMinutes(2)) < 0; // Strictly less than 2 minutes
            boolean hasNoNote = work.getNote() == null || work.getNote().isBlank();
            boolean isToday = entry.getKey().equals(LocalDate.now());
            return !isToday && isShort && hasNoNote;
        });
    }
}