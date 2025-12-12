package com.chrono.task.persistence;

import com.chrono.task.model.Settings;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

public class SettingsStorageService {

    private final File file;
    private final ObjectMapper mapper;

    public SettingsStorageService() {
        String userHome = System.getProperty("user.home");
        this.file = new File(userHome, ".chrono-task-ai.json");
        this.mapper = new ObjectMapper();
    }

    public Settings load() throws IOException {
        if (!file.exists()) {
            return new Settings();
        }
        return mapper.readValue(file, Settings.class);
    }

    public void save(Settings settings) throws IOException {
        mapper.writerWithDefaultPrettyPrinter().writeValue(file, settings);
    }
}
