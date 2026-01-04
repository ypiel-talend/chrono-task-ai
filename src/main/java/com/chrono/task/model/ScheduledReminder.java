package com.chrono.task.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScheduledReminder {

    @Builder.Default
    private String id = UUID.randomUUID().toString();

    private String title;

    @Builder.Default
    private String description = "";

    @Builder.Default
    private String link = "";

    private ReminderType type;

    // For ONE_TIME reminders
    private LocalDate date;
    private LocalTime time;

    // For WEEKLY reminders
    private DayOfWeek dayOfWeek;
    private LocalTime weeklyTime;

    @Builder.Default
    private boolean enabled = true;

    public enum ReminderType {
        ONE_TIME,
        WEEKLY
    }
}