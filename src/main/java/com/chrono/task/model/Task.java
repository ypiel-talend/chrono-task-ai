package com.chrono.task.model;

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
    private Map<LocalDate, Duration> timeHistory = new HashMap<>();

    @Builder.Default
    private String markdownContent = "";

    /**
     * Helper to add time to a specific date.
     */
    public void addTime(LocalDate date, Duration duration) {
        timeHistory.merge(date, duration, Duration::plus);
    }


    public void setTime(LocalDate date, Duration duration) {
        timeHistory.put(date, duration);
    }

    public Duration getTotalTime() {
        return timeHistory.values().stream()
                .reduce(Duration.ZERO, Duration::plus);
    }

    public Duration getTimeForDate(LocalDate date) {
        return timeHistory.getOrDefault(date, Duration.ZERO);
    }

    public Duration getDurationLast30Days() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(30);
        return timeHistory.entrySet().stream()
                .filter(entry -> !entry.getKey().isBefore(start) && !entry.getKey().isAfter(today))
                .map(Map.Entry::getValue)
                .reduce(Duration.ZERO, Duration::plus);
    }

    public Duration getDurationToday() {
        return getTimeForDate(LocalDate.now());
    }

    public String getLabel(){
        StringBuilder sb = new StringBuilder();
        sb.append(this.getOrder()).append(" - ");
        if(isJira){
            sb.append(this.getJiraUrl().substring(this.getJiraUrl().lastIndexOf('/') + 1));
            sb.append(": ");
        }
        sb.append(this.getDescription());
        return sb.toString();
    }
}