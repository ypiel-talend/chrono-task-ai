package com.chrono.task.persistence;

import com.chrono.task.model.DataStore;
import com.chrono.task.model.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class JsonStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void testSaveAndLoad() throws IOException {
        Path jsonFile = tempDir.resolve("test_data.json");
        JsonStorageService service = new JsonStorageService(jsonFile.toString());

        DataStore ds = new DataStore();
        Task t1 = Task.builder().description("Test1").build();
        t1.addTime(LocalDate.now(), Duration.ofMinutes(30));
        ds.getTasks().add(t1);

        service.save(ds);

        assertTrue(jsonFile.toFile().exists());

        DataStore loaded = service.load();
        assertEquals(1, loaded.getTasks().size());
        Task t2 = loaded.getTasks().get(0);
        assertEquals("Test1", t2.getDescription());
        assertEquals(30, t2.getTotalTime().toMinutes());
    }
}
