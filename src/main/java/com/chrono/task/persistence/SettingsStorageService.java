package com.chrono.task.persistence;

import com.chrono.task.model.Settings;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SettingsStorageService {

    private final File file;
    private final ObjectMapper mapper;

    public SettingsStorageService() {
        String userHome = System.getProperty("user.home");
        String settingsFileProperty = System.getProperty("chronotaskai.settings.file");
        this.file = settingsFileProperty != null ? new File (settingsFileProperty) :
                new File(userHome, ".chrono-task-ai.settings.json");
        this.mapper = new ObjectMapper();
    }

    public Settings load() throws IOException {
        log.info("Chrono-task-ai load setting from %s".formatted(this.file.getAbsolutePath()));
        if (!file.exists()) {
            return new Settings();
        }
        return mapper.readValue(file, Settings.class);
    }

    public void save(Settings settings) throws IOException {
        mapper.writerWithDefaultPrettyPrinter().writeValue(file, settings);
    }
}