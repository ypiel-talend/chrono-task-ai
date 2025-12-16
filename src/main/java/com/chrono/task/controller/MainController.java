package com.chrono.task.controller;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.web.WebView;
import javafx.util.Duration;

import com.chrono.task.model.Task;
import com.chrono.task.model.TaskDailyWork;
import com.chrono.task.model.TaskStatus;
import com.chrono.task.service.JiraService.IssueInfo;
import com.chrono.task.service.TaskService;
import com.chrono.task.service.TimerService;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;

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
    private javafx.scene.control.Button pauseButton;
    @FXML
    private TextField filterField;
    @FXML
    private ListView<Task> taskListView;
    @FXML
    private TextArea markdownEditor;
    @FXML
    private TextArea dailyNoteArea;
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
    @FXML
    private TextField jiraApiTokenField;
    @FXML
    private TextField jiraEmailField;

    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().build();

    private final javafx.application.HostServices hostServices;
    private final com.chrono.task.persistence.SettingsStorageService settingsService;
    private final com.chrono.task.model.Settings settings;
    private final com.chrono.task.service.JiraService jiraService;

    private static final String totalTimerFormat = "Total: %02d:%02d";;
    private static final String monthlyTimerFormat = "30d: %02d:%02d";;
    private static final String dailyTimerFormat = "Today: %02d:%02d:%02d";;

    public MainController(TaskService taskService, TimerService timerService,
            com.chrono.task.persistence.SettingsStorageService settingsService,
            com.chrono.task.model.Settings settings,
            javafx.application.HostServices hostServices,
            com.chrono.task.service.JiraService jiraService) {
        this.taskService = taskService;
        this.timerService = timerService;
        this.settingsService = settingsService;
        this.settings = settings;
        this.hostServices = hostServices;
        this.jiraService = jiraService;
    }

    @FXML
    public void initialize() {
        // Bind Task List
        taskListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
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

        dailyNoteArea.textProperty().addListener((obs, o, n) -> {
            Task current = taskListView.getSelectionModel().getSelectedItem();
            if (current != null) {
                current.setDailyNote(LocalDate.now(), n);
            }
        });

        descriptionField.textProperty().addListener((obs, o, n) -> {
            Task current = taskListView.getSelectionModel().getSelectedItem();
            if (current != null) {
                try {
                    taskService.updateTaskDescription(current, n);
                } catch (IllegalArgumentException e) {
                    showPopup("Validation Error", "Invalid Description: " + e.getMessage());
                    // Revert the field to the valid value to prevent inconsistency
                    // runLater to avoid interference with current event processing
                    javafx.application.Platform.runLater(() -> descriptionField.setText(current.getDescription()));
                    return;
                }
                taskListView.refresh(); // Refresh list to show new name

                // Jira Detection
                if (jiraService.isJiraUrl(n)) {
                    String email = settings.getJiraEmail();
                    String token = settings.getJiraApiToken();

                    if (email != null && !email.isBlank() && token != null && !token.isBlank()) {
                        jiraService.fetchIssue(n, email, token)
                                .thenAccept(issue -> {
                                    javafx.application.Platform.runLater(() -> {
                                        // Update Model
                                        try {
                                            taskService.updateTaskDescription(current, issue.summary);
                                            taskService.updateTaskJiraUrl(current, n);
                                            current.setStatus(jiraService.mapStatus(issue.status));
                                        } catch (IllegalArgumentException e) {
                                            showPopup("Validation Error", "Update Failed: " + e.getMessage());
                                            return;
                                        }
                                        current.setJira(true);

                                        // Update UI
                                        descriptionField.setText(current.getDescription());
                                        jiraUrlField.setText(current.getJiraUrl());
                                        taskListView.refresh();
                                    });
                                })
                                .exceptionally(ex -> {
                                    javafx.application.Platform.runLater(() -> {
                                        showPopup("Jira Error", "Failed to fetch Jira Issue: " + ex.getMessage());
                                    });
                                    return null;
                                });
                    }
                }
            }
        });

        jiraUrlField.textProperty().addListener((obs, o, n) -> {
            Task current = taskListView.getSelectionModel().getSelectedItem();
            if (current != null) {
                try {
                    taskService.updateTaskJiraUrl(current, n);
                } catch (IllegalArgumentException e) {
                    showPopup("Validation Error", "Invalid URL: " + e.getMessage());
                    javafx.application.Platform.runLater(() -> jiraUrlField.setText(current.getJiraUrl()));
                    return;
                }
                Optional<IssueInfo> issueInfo = jiraService.parseUrl(n);
                current.setJira(issueInfo.isPresent());
            }
        });

        // History Date Picker
        historyDatePicker.setValue(LocalDate.now());
        historyDatePicker.valueProperty().addListener((obs, o, n) -> loadHistory(n));
        loadHistory(LocalDate.now()); // Initial load

        // Settings
        if (jiraApiTokenField != null) {
            jiraApiTokenField.setText(settings.getJiraApiToken());
        }
        if (jiraEmailField != null) {
            jiraEmailField.setText(settings.getJiraEmail());
        }

        // Bind Pause Button
        if (pauseButton != null) {
            pauseButton.disableProperty().bind(timerService.activeTaskProperty().isNull());
            pauseButton.textProperty().bind(
                    javafx.beans.binding.Bindings.when(timerService.pausedProperty())
                            .then("Resume")
                            .otherwise("Pause"));
        }
    }

    @FXML
    public void onSaveSettings() {
        settings.setJiraApiToken(jiraApiTokenField.getText());
        settings.setJiraEmail(jiraEmailField.getText());
        try {
            settingsService.save(settings);
            showPopup("Settings Saved", "Settings have been saved successfully.");
        } catch (java.io.IOException e) {
            e.printStackTrace();
            showPopup("Error", "Could not save settings: " + e.getMessage());
        }
    }

    private void showPopup(String title, String message) {
        Alert.AlertType type = Alert.AlertType.INFORMATION;
        if (title.toLowerCase().contains("error") || message.toLowerCase().contains("failed")
                || message.toLowerCase().contains("invalid")) {
            type = Alert.AlertType.ERROR;
        }
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }

    @FXML
    public void onAddTask() {
        int index = taskService.getTasks().size() + 1;
        String name = "New Task " + index;
        while (true) {
            try {
                taskService.createTask(name);
                break;
            } catch (IllegalArgumentException e) {
                index++;
                name = "New Task " + index;
            }
        }
    }

    private void loadTaskDetails(Task task) {
        if (task == null) {
            descriptionField.setText("");
            jiraUrlField.setText("");
            markdownEditor.setText("");
            dailyNoteArea.setText("");
            markdownPreview.getEngine().loadContent("");
            return;
        }
        descriptionField.setText(task.getDescription());
        jiraUrlField.setText(task.getJiraUrl());
        markdownEditor.setText(task.getMarkdownContent());
        dailyNoteArea.setText(task.getDailyNote(LocalDate.now()));
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
                current.setTime(LocalDate.now(), java.time.Duration.ofMinutes(minutes));
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
            StringBuilder sb = new StringBuilder();
            sb.append(current.getMarkdownContent());

            if (!current.getTaskHistory().isEmpty()) {
                sb.append("\n\n---\n\n");
                sb.append("# Daily Notes\n\n");

                current.getTaskHistory().entrySet().stream()
                        .sorted(java.util.Comparator.<Map.Entry<LocalDate, TaskDailyWork>, LocalDate>comparing(
                                Map.Entry::getKey)
                                .reversed())
                        .forEach(entry -> {
                            java.time.LocalDate date = entry.getKey();
                            com.chrono.task.model.TaskDailyWork work = entry.getValue();

                            java.time.Duration d = work.getDuration();
                            String durationStr = String.format("%02dh %02dm", d.toHours(), d.toMinutesPart());

                            sb.append("## ").append(date).append(" (").append(durationStr).append(")\n\n");
                            if (work.getNote() != null && !work.getNote().isBlank()) {
                                sb.append(work.getNote()).append("\n\n");
                            } else {
                                sb.append("*No note*\n\n");
                            }
                        });
            }

            String html = renderer.render(parser.parse(sb.toString()));
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

            activeTimerLabel.setText(String.format(totalTimerFormat,
                    d.toHours(), d.toMinutesPart(), d.toSecondsPart()));

            java.time.Duration today = current.getDurationToday();
            todayTimerLabel.setText(String.format(dailyTimerFormat,
                    today.toHours(), today.toMinutesPart(), today.toSecondsPart()));

            java.time.Duration month = current.getDurationLast30Days();
            monthTimerLabel.setText(String.format(monthlyTimerFormat,
                    month.toHours(), month.toMinutesPart(), month.toSecondsPart()));
        } else {
            activeTimerLabel.setText(String.format(totalTimerFormat, 0, 0));
            todayTimerLabel.setText(String.format(dailyTimerFormat, 0, 0, 0));
            monthTimerLabel.setText(String.format(monthlyTimerFormat, 0, 0));
        }
    }

    @FXML
    public void onTogglePause() {
        if (timerService.isPaused()) {
            timerService.resume();
        } else {
            timerService.pause();
        }
        updateTimerLabel(); // Immediate UI feedback
    }

    @FXML
    private javafx.scene.control.CheckBox historyDurationCheckbox;

    @FXML
    public void onRefreshHistory() {
        loadHistory(historyDatePicker.getValue());
    }

    private void loadHistory(LocalDate date) {
        if (date == null)
            return;
        StringBuilder sb = new StringBuilder();
        sb.append("History for ").append(date).append("\n\n");

        boolean showDuration = historyDurationCheckbox.isSelected();

        for (Task t : taskService.getTasks()) {
            java.time.Duration d = t.getTimeForDate(date);
            if (d.getSeconds() > 120) {
                sb.append("- ").append(t.getHistoryLabel());
                if (showDuration) {
                    sb.append(String.format(" : %02dh %02dm", d.toHours(), d.toMinutesPart()));
                }
                sb.append("\n");
            }
        }
        historyTextArea.setText(sb.toString());
    }

    // Inner class for drag and drop cell
    private class TaskListCell extends ListCell<Task> {

        private final HBox hbox = new HBox(10);
        private final Label label = new Label();
        private final Label statusLabel = new Label();
        private final javafx.scene.layout.Region spacer = new javafx.scene.layout.Region(); // Not used in current
                                                                                            // layout but kept if needed
                                                                                            // or removed

        public TaskListCell() {
            hbox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            // Layout configuration
            label.setMaxWidth(Double.MAX_VALUE);
            label.setMinWidth(0);
            HBox.setHgrow(label, Priority.ALWAYS);

            statusLabel.setStyle("-fx-font-weight: bold; -fx-padding: 0 10 0 10;");
            statusLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);

            hbox.getChildren().addAll(label, statusLabel);

            // Bind HBox width to Cell width to ensure truncation works
            // Subtracting logic to account for padding/scrollbars
            hbox.prefWidthProperty().bind(widthProperty().subtract(20));

            setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && getItem() != null) {
                    String url = getItem().getJiraUrl();
                    if (url != null && !url.isBlank() && (url.startsWith("http://") || url.startsWith("https://"))) {
                        hostServices.showDocument(url);
                    }
                }
            });

            // Logic for status label click
            statusLabel.setOnMouseClicked(e -> {
                Task item = getItem();
                if (item != null && item.isJira() && item.getJiraUrl() != null) {
                    java.lang.String email = settings.getJiraEmail();
                    java.lang.String token = settings.getJiraApiToken();
                    if (email != null && !email.isBlank() && token != null && !token.isBlank()) {
                        statusLabel.setText("Updating...");
                        jiraService.fetchIssue(item.getJiraUrl(), email, token)
                                .thenAccept(issue -> {
                                    javafx.application.Platform.runLater(() -> {
                                        TaskStatus newStatus = jiraService.mapStatus(issue.status);
                                        item.setStatus(newStatus);
                                        statusLabel.setText(newStatus.name());
                                        taskListView.refresh();
                                    });
                                })
                                .exceptionally(ex -> {
                                    javafx.application.Platform.runLater(() -> {
                                        statusLabel.setText(item.getStatus().name());
                                        showPopup("Jira Error", "Failed to refresh status: " + ex.getMessage());
                                    });
                                    return null;
                                });
                    }
                }
                e.consume();
            });

            setOnDragDetected(event -> {
                Task toMove = taskListView.getSelectionModel().getSelectedItem();
                if (toMove == null) {
                    return;
                }
                javafx.scene.input.Dragboard dragboard = startDragAndDrop(javafx.scene.input.TransferMode.MOVE);
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(toMove.getId());
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
                    String draggedId = db.getString();
                    Optional<Task> toMove = items.stream().filter(t -> t.getId().equals(draggedId)).findAny();

                    if (toMove.isPresent()) {
                        items.remove(toMove.get());
                        items.add(getIndex(), toMove.get());
                        taskService.updateOrder(items);
                        taskListView.getSelectionModel().select(getIndex());
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
                setText(null);
                label.setText(item.getLabel());
                statusLabel.setText(item.getStatus().name());
                setGraphic(hbox);
            }
        }
    }
}