package com.chrono.task.service;

import com.chrono.task.model.Task;
import com.chrono.task.persistence.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaskServiceTest {

    private StorageService storageMock;
    private TaskService service;

    @BeforeEach
    void setup() {
        storageMock = Mockito.mock(StorageService.class);
        service = new TaskService(storageMock);
    }

    @Test
    void testCreateTask() {
        Task t = service.createTask("Foo");
        assertEquals("Foo", t.getDescription());
        assertEquals(0, t.getOrder());
        assertEquals(1, service.getTasks().size());
    }

    @Test
    void testReorder() {
        Task t1 = service.createTask("A");
        Task t2 = service.createTask("B");

        // Swap
        service.updateOrder(List.of(t2, t1));

        assertEquals(0, t2.getOrder());
        assertEquals(1, t1.getOrder());
        assertEquals("B", service.getTasks().get(0).getDescription());
    }

    @Test
    void testFilter() {
        service.createTask("Buy Milk");
        service.createTask("Walk Dog");

        assertEquals(1, service.filter("Milk").size());
        assertEquals(2, service.filter("").size());
        assertEquals(0, service.filter("Cat").size());
    }

    @Test
    void testReorderWithSelf() {
        service.createTask("Task 1");
        service.createTask("Task 2");

        // Simulate what Drag operations do often: pass the list itself or a list that
        // is the same instance
        // TaskService.updateOrder calls tasks.setAll(newOrder)
        // If newOrder == tasks, setAll clears it first.

        service.updateOrder(service.getTasks());

        assertFalse(service.getTasks().isEmpty(), "Tasks list should not be empty after reordering with itself");
        assertEquals(2, service.getTasks().size());
    }

    @Test
    void testDuplicateDescriptionOnCreate() {
        service.createTask("Task A");
        assertThrows(IllegalArgumentException.class, () -> service.createTask("Task A"));
    }

    @Test
    void testDuplicateDescriptionOnUpdate() {
        Task t1 = service.createTask("Task A");
        Task t2 = service.createTask("Task B");

        assertThrows(IllegalArgumentException.class, () -> service.updateTaskDescription(t2, "Task A"));

        // Self update should be fine
        assertDoesNotThrow(() -> service.updateTaskDescription(t1, "Task A"));
    }

    @Test
    void testDuplicateJiraUrlOnUpdate() {
        Task t1 = service.createTask("Task A");
        service.updateTaskJiraUrl(t1, "http://jira.com/1");

        Task t2 = service.createTask("Task B");
        assertThrows(IllegalArgumentException.class, () -> service.updateTaskJiraUrl(t2, "http://jira.com/1"));

        // Unique url is fine
        assertDoesNotThrow(() -> service.updateTaskJiraUrl(t2, "http://jira.com/2"));
    }
}
