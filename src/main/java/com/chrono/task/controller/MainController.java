package com.chrono.task.controller;

import com.chrono.task.model.Task;
import com.chrono.task.service.TaskService;
import com.chrono.task.service.TimerService;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.web.WebView;
import javafx.util.Duration;

import java.time.LocalDate;

public class MainController {

    private final TaskService taskService;
    private final TimerService timerService;

    @FXML
    private Label activeTaskLabel;
    @FXML
    private Label activeTimerLabel;
    @FXML
    private Label todayTimerLabel;
    @FXML
    private Label monthTimerLabel;
    @FXML
    private TextField filterField;
    @FXML
    private ListView<Task> taskListView;
    @FXML
    private TextArea markdownEditor;
    @FXML
    private WebView markdownPreview;

    // History Tab
    @FXML
    private DatePicker historyDatePicker;
    @FXML
    private TextArea historyTextArea;

    // Fields for task details editing
    @FXML
    private TextField descriptionField;
    @FXML
    private TextField jiraUrlField;

    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().build();

    public MainController(TaskService taskService, TimerService timerService) {
        this.taskService = taskService;
        this.timerService = timerService;
    }

    @FXML
    public void initialize() {
        // Bind Task List
        taskListView.setItems(taskService.getTasks());
        taskListView.setCellFactory(param -> new TaskListCell());

        // Filter
        filterField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isBlank()) {
                taskListView.setItems(taskService.getTasks());
            } else {
                taskListView.setItems(javafx.collections.FXCollections.observableArrayList(taskService.filter(newVal)));
            }
        });

        // Selection Listener
        taskListView.getSelectionModel().selectedItemProperty().addListener((obs, oldTask, newTask) -> {
            timerService.setActiveTask(newTask);
            loadTaskDetails(newTask);
        });

        // Active Task Header
        timerService.activeTaskProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                activeTaskLabel.setText("Active: " + newVal.getDescription());
            } else {
                activeTaskLabel.setText("No Active Task");
            }
        });

        // Periodic UI update for timer (1 second)
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateTimerLabel()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        // Markdown Auto-Refresh (3 seconds)
        Timeline markdownTimer = new Timeline(new KeyFrame(Duration.seconds(3), e -> refreshMarkdown()));
        markdownTimer.setCycleCount(Timeline.INDEFINITE);
        markdownTimer.play();

        // Editor listeners to update model
        markdownEditor.textProperty().addListener((obs, o, n) -> {
            Task current = taskListView.getSelectionModel().getSelectedItem();
            if (current != null) {
                current.setMarkdownContent(n);
            }
        });

        descriptionField.textProperty().addListener((obs, o, n) -> {
            Task current = taskListView.getSelectionModel().getSelectedItem();
            if (current != null) {
                current.setDescription(n);
                taskListView.refresh(); // Refresh list to show new name
            }
        });

        jiraUrlField.textProperty().addListener((obs, o, n) -> {
            Task current = taskListView.getSelectionModel().getSelectedItem();
            if (current != null) {
                current.setJiraUrl(n);
            }
        });

        // History Date Picker
        historyDatePicker.setValue(LocalDate.now());
        historyDatePicker.valueProperty().addListener((obs, o, n) -> loadHistory(n));
        loadHistory(LocalDate.now()); // Initial load
    }

    @FXML
    public void onAddTask() {
        taskService.createTask("New Task " + (taskService.getTasks().size() + 1));
    }

    private void loadTaskDetails(Task task) {
        if (task == null) {
            descriptionField.setText("");
            jiraUrlField.setText("");
            markdownEditor.setText("");
            markdownPreview.getEngine().loadContent("");
            return;
        }
        descriptionField.setText(task.getDescription());
        jiraUrlField.setText(task.getJiraUrl());
        markdownEditor.setText(task.getMarkdownContent());
        refreshMarkdown(); // Immediate refresh
    }

    @FXML
    private TextField timeAdjustmentField;

    @FXML
    public void onAdjustTime() {
        Task current = taskListView.getSelectionModel().getSelectedItem();
        if (current != null) {
            String text = timeAdjustmentField.getText();
            try {
                long minutes = Long.parseLong(text);
                current.addTime(LocalDate.now(), java.time.Duration.ofMinutes(minutes));
                updateTimerLabel(); // Refresh view immediately
                timeAdjustmentField.clear();
            } catch (NumberFormatException e) {
                // Ignore or show alert
                System.err.println("Invalid number: " + text);
            }
        }
    }

    private void refreshMarkdown() {
        Task current = taskListView.getSelectionModel().getSelectedItem();
        if (current != null) {
            String html = renderer.render(parser.parse(current.getMarkdownContent()));
            markdownPreview.getEngine().loadContent(html);
        }
    }

    private void updateTimerLabel() {
        Task current = timerService.activeTaskProperty().get();
        if (current != null) {
            // Re-calculate total time from history including ongoing session is implied by
            // timerService tick
            // Ideally TimerService provides "current session time" + "historical time"
            // But TimerService ticks every second updating the model directly.
            // So we can just read the model.
            java.time.Duration d = current.getTotalTime();
            activeTimerLabel.setText(String.format("%02d:%02d:%02d",
                    d.toHours(), d.toMinutesPart(), d.toSecondsPart()));

            java.time.Duration today = current.getDurationToday();
            todayTimerLabel.setText(String.format("Today: %dh %02dm", today.toHours(), today.toMinutesPart()));

            java.time.Duration month = current.getDurationLast30Days();
            monthTimerLabel.setText(String.format("30d: %dh %02dm", month.toHours(), month.toMinutesPart()));
        } else {
            activeTimerLabel.setText("00:00:00");
            todayTimerLabel.setText("Today: 0h 00m");
            monthTimerLabel.setText("30d: 0h 00m");
        }
    }

    private void loadHistory(LocalDate date) {
        if (date == null)
            return;
        StringBuilder sb = new StringBuilder();
        sb.append("History for ").append(date).append("\n\n");

        for (Task t : taskService.getTasks()) {
            java.time.Duration d = t.getTimeForDate(date);
            if (d.getSeconds() > 120) {
                sb.append(String.format("- %s : %02dh %02dm\n",
                        t.getDescription(), d.toHours(), d.toMinutesPart()));
            }
        }
        historyTextArea.setText(sb.toString());
    }

    // Inner class for drag and drop cell
    private class TaskListCell extends ListCell<Task> {

        public TaskListCell() {
            setOnDragDetected(event -> {
                if (getItem() == null) {
                    return;
                }
                javafx.scene.input.Dragboard dragboard = startDragAndDrop(javafx.scene.input.TransferMode.MOVE);
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(getItem().getId());
                dragboard.setContent(content);
                event.consume();
            });

            setOnDragOver(event -> {
                if (event.getGestureSource() != this && event.getDragboard().hasString()) {
                    event.acceptTransferModes(javafx.scene.input.TransferMode.MOVE);
                }
                event.consume();
            });

            setOnDragEntered(event -> {
                if (event.getGestureSource() != this && event.getDragboard().hasString()) {
                    setOpacity(0.3);
                }
            });

            setOnDragExited(event -> {
                if (event.getGestureSource() != this && event.getDragboard().hasString()) {
                    setOpacity(1);
                }
            });

            setOnDragDropped(event -> {
                if (getItem() == null) {
                    return;
                }
                javafx.scene.input.Dragboard db = event.getDragboard();
                boolean success = false;
                if (db.hasString()) {
                    javafx.collections.ObservableList<Task> items = getListView().getItems();
                    int draggedIdx = -1;
                    String draggedId = db.getString();

                    // Find index of dragged item
                    for (int i = 0; i < items.size(); i++) {
                        if (items.get(i).getId().equals(draggedId)) {
                            draggedIdx = i;
                            break;
                        }
                    }

                    int thisIdx = getIndex();

                    if (draggedIdx != -1 && draggedIdx != thisIdx) {
                        Task itemOffset = items.remove(draggedIdx);
                        items.add(thisIdx, itemOffset);

                        // Update Order in Service
                        taskService.updateOrder(items);
                        success = true;
                    }
                }
                event.setDropCompleted(success);
                event.consume();
            });

            setOnDragDone(javafx.scene.input.DragEvent::consume);
        }

        @Override
        protected void updateItem(Task item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                setText(item.getDescription() != null ? item.getDescription() : "Untitled");
            }
        }
    }
}
