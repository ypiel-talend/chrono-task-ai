package com.chrono.task.model;

import java.time.temporal.ChronoUnit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Settings {
    private String jiraApiToken;
    private String jiraEmail;

    private String dataStoragePath = System.getProperty("user.home") + java.io.File.separator + ".chrono-task-ai";
    private long gitBackupInterval = 1;
    private ChronoUnit gitBackupUnit = ChronoUnit.HOURS;
    private boolean gitBackupEnabled = false;
    private String markdownFont = "System";
}
