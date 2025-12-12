package com.chrono.task.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDailyWork {

    @Builder.Default
    private Duration duration = Duration.ZERO;

    @Builder.Default
    private String note = "";

    @Builder.Default
    private TaskStatus status = TaskStatus.TODO;
}
