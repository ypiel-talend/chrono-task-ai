package com.chrono.task.persistence;

import com.chrono.task.model.SchedulingData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;

@Slf4j
public class SchedulingStorageService {

    private final String filePath;
    private final ObjectMapper objectMapper;

    public SchedulingStorageService(String filePath) {
        this.filePath = filePath;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public SchedulingData load() {
        File file = new File(filePath);
        if (!file.exists()) {
            log.info("Scheduling data file not found. Creating new SchedulingData.");
            return SchedulingData.builder().build();
        }

        try {
            return objectMapper.readValue(file, SchedulingData.class);
        } catch (IOException e) {
            log.error("Failed to load scheduling data from {}", filePath, e);
            return SchedulingData.builder().build();
        }
    }

    public void save(SchedulingData schedulingData) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(filePath), schedulingData);
            log.info("Scheduling data saved to {}", filePath);
        } catch (IOException e) {
            log.error("Failed to save scheduling data to {}", filePath, e);
        }
    }
}