package com.chrono.task.persistence;

import com.chrono.task.model.DataStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;

public class JsonStorageService implements StorageService {

    private final File file;
    private final ObjectMapper mapper;

    public JsonStorageService(String filePath) {
        this.file = new File(filePath);
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
    }

    @Override
    public DataStore load() throws IOException {
        if (!file.exists()) {
            return new DataStore();
        }
        return mapper.readValue(file, DataStore.class);
    }

    @Override
    public void save(DataStore data) throws IOException {
        mapper.writerWithDefaultPrettyPrinter().writeValue(file, data);
    }
}
