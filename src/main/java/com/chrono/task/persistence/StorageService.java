package com.chrono.task.persistence;

import com.chrono.task.model.DataStore;

import java.io.IOException;

public interface StorageService {
    DataStore load() throws IOException;
    void save(DataStore data) throws IOException;
}
