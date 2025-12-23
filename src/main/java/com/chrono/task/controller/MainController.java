package com.chrono.task.controller;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.web.WebView;
import javafx.scene.text.Font;
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
    private Label totalDailyLabel;
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
    @FXML
    private Label lastSaveLabel;
    @FXML
    private Label lastCommitLabel;

    // History Tab
    @FXML
    private DatePicker historyDatePicker;
    @FXML
    private Label historyEndLabel;
    @FXML
    private DatePicker historyEndDatePicker;
    @FXML
    private javafx.scene.control.CheckBox historyRangeCheckbox;
    @FXML
    private TextArea historyTextArea;

    // Fields for task details editing
    @FXML
    private TextField descriptionField;
    @FXML
    private TextField jiraUrlField;
    @FXML
    private TextField slackUrlField;
    @FXML
    private PasswordField jiraApiTokenField;
    @FXML
    private TextField jiraApiTokenVisibleField;
    @FXML
    private ToggleButton tokenVisibilityToggle;
    @FXML
    private TextField jiraEmailField;
    @FXML
    private ComboBox<TaskStatus> statusComboBox;

    @FXML
    private TextField dataStoragePathField;
    @FXML
    private javafx.scene.control.CheckBox gitBackupEnabledCheckbox;
    @FXML
    private TextField gitBackupIntervalField;
    @FXML
    private ComboBox<java.time.temporal.ChronoUnit> gitBackupUnitComboBox;
    @FXML
    private ComboBox<String> markdownFontComboBox;
    @FXML
    private Label gitStatusLabel;
    @FXML
    private javafx.scene.control.CheckBox jiraRefreshEnabledCheckbox;
    @FXML
    private TextField jiraRefreshIntervalField;
    @FXML
    private ComboBox<java.time.temporal.ChronoUnit> jiraRefreshUnitComboBox;
    @FXML
    private Label jiraUpdateLabel;

    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().build();

    private final javafx.application.HostServices hostServices;
    private final com.chrono.task.persistence.SettingsStorageService settingsService;
    private final com.chrono.task.model.Settings settings;
    private final com.chrono.task.service.JiraService jiraService;
    private final com.chrono.task.service.GitBackupService gitBackupService;
    private final com.chrono.task.service.JiraRefreshService jiraRefreshService;

    private static final String totalTimerFormat = "Total: %02d:%02d";
    private static final String monthlyTimerFormat = "30d: %02d:%02d";
    private static final String dailyTimerFormat = "Today: %02d:%02d:%02d";

    public MainController(TaskService taskService, TimerService timerService,
            com.chrono.task.persistence.SettingsStorageService settingsService,
            com.chrono.task.model.Settings settings,
            javafx.application.HostServices hostServices,
            com.chrono.task.service.JiraService jiraService,
            com.chrono.task.service.GitBackupService gitBackupService,
            com.chrono.task.service.JiraRefreshService jiraRefreshService) {
        this.taskService = taskService;
        this.timerService = timerService;
        this.settingsService = settingsService;
        this.settings = settings;
        this.hostServices = hostServices;
        this.jiraService = jiraService;
        this.gitBackupService = gitBackupService;
        this.jiraRefreshService = jiraRefreshService;
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

        slackUrlField.textProperty().addListener((obs, o, n) -> {
            Task current = taskListView.getSelectionModel().getSelectedItem();
            if (current != null) {
                taskService.updateTaskSlackUrl(current, n);
            }
        });

        // History Date Picker
        historyDatePicker.setValue(LocalDate.now());
        historyDatePicker.valueProperty().addListener((obs, o, n) -> onRefreshHistory());
        historyEndDatePicker.valueProperty().addListener((obs, o, n) -> onRefreshHistory());
        historyRangeCheckbox.selectedProperty().addListener((obs, o, n) -> onRefreshHistory());
        historyDurationCheckbox.selectedProperty().addListener((obs, o, n) -> onRefreshHistory());
        historyDailyNoteCheckbox.selectedProperty().addListener((obs, o, n) -> onRefreshHistory());
        // Status Dropdown
        statusComboBox.getItems().setAll(TaskStatus.values());
        statusComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            Task current = taskListView.getSelectionModel().getSelectedItem();
            if (current != null && newVal != null && current.getStatus() != newVal) {
                current.setStatus(newVal);
                taskListView.refresh();
            }
        });

        loadHistory(LocalDate.now()); // Initial load

        // Settings
        if (jiraApiTokenField != null) {
            jiraApiTokenField.setText(settings.getJiraApiToken());

            // Sync visible field with password field
            jiraApiTokenVisibleField.textProperty().bindBidirectional(jiraApiTokenField.textProperty());

            // Toggle visibility based on button state
            jiraApiTokenField.visibleProperty().bind(tokenVisibilityToggle.selectedProperty().not());
            jiraApiTokenField.managedProperty().bind(tokenVisibilityToggle.selectedProperty().not());

            jiraApiTokenVisibleField.visibleProperty().bind(tokenVisibilityToggle.selectedProperty());
            jiraApiTokenVisibleField.managedProperty().bind(tokenVisibilityToggle.selectedProperty());
        }
        if (jiraEmailField != null) {
            jiraEmailField.setText(settings.getJiraEmail());
        }
        if (jiraRefreshEnabledCheckbox != null) {
            jiraRefreshEnabledCheckbox.setSelected(settings.isJiraRefreshEnabled());
        }
        if (jiraRefreshIntervalField != null) {
            jiraRefreshIntervalField.setText(String.valueOf(settings.getJiraRefreshInterval()));
        }
        if (jiraRefreshUnitComboBox != null) {
            jiraRefreshUnitComboBox.getItems().setAll(
                    java.time.temporal.ChronoUnit.HOURS,
                    java.time.temporal.ChronoUnit.MINUTES,
                    java.time.temporal.ChronoUnit.SECONDS);
            jiraRefreshUnitComboBox.setValue(settings.getJiraRefreshUnit());
        }

        if (jiraUpdateLabel != null && jiraRefreshService != null) {
            jiraUpdateLabel.visibleProperty().bind(jiraRefreshService.isRefreshingProperty());
            jiraUpdateLabel.managedProperty().bind(jiraRefreshService.isRefreshingProperty());
        }

        // Custom Settings
        if (dataStoragePathField != null) {
            dataStoragePathField.setText(settings.getDataStoragePath());
            gitBackupEnabledCheckbox.setSelected(settings.isGitBackupEnabled());
            gitBackupIntervalField.setText(String.valueOf(settings.getGitBackupInterval()));
            gitBackupUnitComboBox.getItems().setAll(
                    java.time.temporal.ChronoUnit.DAYS,
                    java.time.temporal.ChronoUnit.HOURS,
                    java.time.temporal.ChronoUnit.MINUTES);
            gitBackupUnitComboBox.setValue(settings.getGitBackupUnit());
            updateGitStatusLabel();
        }

        if (markdownFontComboBox != null) {
            markdownFontComboBox.getItems().setAll(Font.getFamilies());
            markdownFontComboBox.setValue(settings.getMarkdownFont());
        }

        // Bind Pause Button
        if (pauseButton != null) {
            pauseButton.disableProperty().bind(timerService.activeTaskProperty().isNull());
            pauseButton.textProperty().bind(
                    javafx.beans.binding.Bindings.when(timerService.pausedProperty())
                            .then("Resume")
                            .otherwise("Pause"));
        }

        // Status Bar Bindings
        taskService.lastSaveTimeProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                lastSaveLabel.setText(
                        "Last save: " + newVal.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
            }
        });

        if (gitBackupService != null) {
            lastCommitLabel.visibleProperty().bind(gitBackupEnabledCheckbox.selectedProperty());
            lastCommitLabel.managedProperty().bind(gitBackupEnabledCheckbox.selectedProperty());

            gitBackupService.lastCommitMessageProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    lastCommitLabel.setText("Last commit: " + newVal);
                }
            });
            // Set initial value
            String lastMsg = gitBackupService.lastCommitMessageProperty().get();
            if (lastMsg != null) {
                lastCommitLabel.setText("Last commit: " + lastMsg);
            }
        }

        // Initialize End Date Picker
        historyEndDatePicker.setValue(LocalDate.now());
    }

    @FXML
    public void onSaveSettings() {
        settings.setJiraApiToken(jiraApiTokenField.getText());
        settings.setJiraEmail(jiraEmailField.getText());
        settings.setDataStoragePath(dataStoragePathField.getText());
        settings.setGitBackupEnabled(gitBackupEnabledCheckbox.isSelected());
        try {
            settings.setGitBackupInterval(Long.parseLong(gitBackupIntervalField.getText()));
        } catch (NumberFormatException e) {
            showPopup("Validation Error", "Invalid Backup Interval: Must be a number.");
            return;
        }
        settings.setGitBackupUnit(gitBackupUnitComboBox.getValue());
        settings.setMarkdownFont(markdownFontComboBox.getValue());

        settings.setJiraRefreshEnabled(jiraRefreshEnabledCheckbox.isSelected());
        try {
            settings.setJiraRefreshInterval(Long.parseLong(jiraRefreshIntervalField.getText()));
        } catch (NumberFormatException e) {
            showPopup("Validation Error", "Invalid Jira Refresh Interval: Must be a number.");
            return;
        }
        settings.setJiraRefreshUnit(jiraRefreshUnitComboBox.getValue());

        try {
            settingsService.save(settings);
            updateGitStatusLabel();
            if (gitBackupService != null) {
                gitBackupService.restart();
            }
            if (jiraRefreshService != null) {
                jiraRefreshService.restart();
            }
            showPopup("Settings Saved", "Settings have been saved successfully.");
            // Notify App to possibly restart backup service
            // This could be improved by using an event system or a direct call if we have
            // reference to the service
        } catch (java.io.IOException e) {
            e.printStackTrace();
            showPopup("Error", "Could not save settings: " + e.getMessage());
        }
    }

    @FXML
    public void onBrowseDataPath() {
        javafx.stage.DirectoryChooser directoryChooser = new javafx.stage.DirectoryChooser();
        directoryChooser.setTitle("Select Data Storage Directory");
        java.io.File selectedDirectory = directoryChooser.showDialog(dataStoragePathField.getScene().getWindow());
        if (selectedDirectory != null) {
            dataStoragePathField.setText(selectedDirectory.getAbsolutePath());
        }
    }

    private void updateGitStatusLabel() {
        if (gitStatusLabel == null)
            return;

        com.chrono.task.service.GitService gitService = new com.chrono.task.service.GitService();
        boolean gitInstalled = gitService.isGitInstalled();
        boolean backupEnabled = gitBackupEnabledCheckbox.isSelected();

        if (backupEnabled && !gitInstalled) {
            gitStatusLabel.setText("Warning: Git is not installed. Backup feature will not work.");
            gitStatusLabel.setVisible(true);
            gitStatusLabel.setManaged(true);
        } else {
            gitStatusLabel.setVisible(false);
            gitStatusLabel.setManaged(false);
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
            slackUrlField.setText("");
            markdownEditor.setText("");
            dailyNoteArea.setText("");
            statusComboBox.setValue(null);
            markdownPreview.getEngine().loadContent("");
            return;
        }
        descriptionField.setText(task.getDescription());
        jiraUrlField.setText(task.getJiraUrl());
        slackUrlField.setText(task.getSlackUrl());
        markdownEditor.setText(task.getMarkdownContent());
        dailyNoteArea.setText(task.getDailyNote(LocalDate.now()));
        statusComboBox.setValue(task.getStatus());
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

            String fontBox = settings.getMarkdownFont();
            String fontFamily = (fontBox == null || "System".equals(fontBox)) ? "sans-serif" : "'" + fontBox + "'";

            String styledHtml = "<html><head><style>" +
                    "body { font-family: " + fontFamily
                    + "; font-size: 14px; line-height: 1.6; color: #333; padding: 20px; }" +
                    "code { font-family: monospace; background-color: #f4f4f4; padding: 2px 4px; border-radius: 4px; }"
                    +
                    "pre { background-color: #f4f4f4; padding: 10px; border-radius: 4px; overflow-x: auto; }" +
                    "h1, h2, h3 { border-bottom: 1px solid #eee; padding-bottom: 5px; }" +
                    "blockquote { border-left: 4px solid #ddd; padding-left: 15px; color: #777; }" +
                    "</style></head><body>" + html + "</body></html>";

            markdownPreview.getEngine().loadContent(styledHtml);
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

        // Calculate total duration for today across all tasks
        java.time.Duration totalToday = java.time.Duration.ZERO;
        for (Task t : taskService.getTasks()) {
            totalToday = totalToday.plus(t.getTimeForDate(LocalDate.now()));
        }
        if (totalDailyLabel != null) {
            totalDailyLabel.setText(String.format("Day Total: %02dh %02dm",
                    totalToday.toHours(), totalToday.toMinutesPart()));
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
    private javafx.scene.control.CheckBox historyDailyNoteCheckbox;

    @FXML
    public void onRefreshHistory() {
        if (historyRangeCheckbox.isSelected()) {
            onGenerateRangeHistory();
        } else {
            loadHistory(historyDatePicker.getValue());
        }
    }

    private void loadHistory(LocalDate date) {
        if (date == null)
            return;
        StringBuilder sb = new StringBuilder();
        sb.append("History for ").append(date).append("\n\n");

        boolean showDuration = historyDurationCheckbox.isSelected();
        boolean showNotes = historyDailyNoteCheckbox.isSelected();

        for (Task t : taskService.getTasks()) {
            java.time.Duration d = t.getTimeForDate(date);
            if (d.getSeconds() > 120
                    || (showNotes && t.getDailyNote(date) != null && !t.getDailyNote(date).isBlank())) {
                sb.append("- ").append(t.getHistoryLabel());
                if (showDuration) {
                    sb.append(String.format(" : %02dh %02dm", d.toHours(), d.toMinutesPart()));
                }
                sb.append("\n");
                if (showNotes) {
                    String note = t.getDailyNote(date);
                    if (note != null && !note.isBlank()) {
                        sb.append("  > ").append(note.replace("\n", "\n  > ")).append("\n");
                    }
                }
            }
        }
        historyTextArea.setText(sb.toString());
    }

    @FXML
    public void onGenerateRangeHistory() {
        LocalDate start = historyDatePicker.getValue();
        LocalDate end = historyEndDatePicker.getValue();

        if (start == null || end == null) {
            showPopup("Error", "Please select both start and end dates.");
            return;
        }

        if (end.isBefore(start)) {
            showPopup("Error", "End date cannot be before start date.");
            return;
        }

        historyTextArea.setText("Generating history... please wait.");

        java.util.List<Task> activeTasks = taskService.getTasks().stream()
                .filter(t -> {
                    for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                        if (t.getTimeForDate(date).getSeconds() > 120) {
                            return true;
                        }
                    }
                    return false;
                })
                .collect(java.util.stream.Collectors.toList());

        if (activeTasks.isEmpty()) {
            historyTextArea.setText("No tasks found for this date range.");
            return;
        }

        String email = settings.getJiraEmail();
        String token = settings.getJiraApiToken();

        boolean showDuration = historyDurationCheckbox.isSelected();
        boolean showNotes = historyDailyNoteCheckbox.isSelected();

        java.util.List<java.util.concurrent.CompletableFuture<String>> futures = activeTasks.stream()
                .map(t -> {
                    java.time.Duration totalRangeDuration = java.time.Duration.ZERO;
                    StringBuilder notesBuilder = new StringBuilder();
                    for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                        totalRangeDuration = totalRangeDuration.plus(t.getTimeForDate(date));
                        if (showNotes) {
                            String note = t.getDailyNote(date);
                            if (note != null && !note.isBlank()) {
                                if (notesBuilder.length() > 0)
                                    notesBuilder.append("\n");
                                notesBuilder.append("  > ").append(date).append(": ")
                                        .append(note.replace("\n", "\n  > "));
                            }
                        }
                    }
                    final String durationStr = showDuration
                            ? String.format(" : %02dh %02dm", totalRangeDuration.toHours(),
                                    totalRangeDuration.toMinutesPart())
                            : "";
                    final String notesStr = notesBuilder.length() > 0 ? "\n" + notesBuilder.toString() : "";

                    if (t.isJira() && t.getJiraUrl() != null && !t.getJiraUrl().isBlank()
                            && email != null && !email.isBlank() && token != null && !token.isBlank()) {
                        return jiraService.fetchIssue(t.getJiraUrl(), email, token)
                                .thenApply(issue -> String.format("%s\t%s\t%s\t%s%s%s", issue.type, t.getJiraUrl(),
                                        issue.status, issue.summary, durationStr, notesStr))
                                .exceptionally(ex -> "Error fetching Jira: " + t.getJiraUrl() + " - " + ex.getMessage()
                                        + durationStr + notesStr);
                    } else {
                        return java.util.concurrent.CompletableFuture
                                .completedFuture(t.getDescription() + durationStr + notesStr);
                    }
                })
                .collect(java.util.stream.Collectors.toList());

        java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0]))
                .thenApply(v -> {
                    return futures.stream()
                            .map(java.util.concurrent.CompletableFuture::join)
                            .collect(java.util.stream.Collectors.joining("\n"));
                })
                .thenAccept(report -> javafx.application.Platform.runLater(() -> historyTextArea.setText(report)))
                .exceptionally(ex -> {
                    javafx.application.Platform
                            .runLater(() -> showPopup("Error", "Failed to generate report: " + ex.getMessage()));
                    return null;
                });
    }

    // Inner class for drag and drop cell
    private class TaskListCell extends ListCell<Task> {

        private final HBox hbox = new HBox(10);
        private final Label label = new Label();
        private final Label statusLabel = new Label();
        private final Button webButton = new Button("🌐");
        private final Button slackButton = new Button("💬");

        public TaskListCell() {
            hbox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            // Layout configuration
            label.setMaxWidth(Double.MAX_VALUE);
            label.setMinWidth(0);
            HBox.setHgrow(label, Priority.ALWAYS);

            statusLabel.setStyle("-fx-font-weight: bold; -fx-padding: 0 0 0 10;");
            statusLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);

            webButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;");
            webButton.setFocusTraversable(false);
            webButton.setOnAction(event -> {
                Task item = getItem();
                if (item != null && item.getJiraUrl() != null && !item.getJiraUrl().isBlank()) {
                    hostServices.showDocument(item.getJiraUrl());
                }
                event.consume();
            });

            slackButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;");
            slackButton.setFocusTraversable(false);
            slackButton.setOnAction(event -> {
                Task item = getItem();
                if (item != null && item.getSlackUrl() != null && !item.getSlackUrl().isBlank()) {
                    hostServices.showDocument(item.getSlackUrl());
                }
                event.consume();
            });

            hbox.getChildren().addAll(label, statusLabel, webButton, slackButton);

            // Bind HBox width to Cell width to ensure truncation works
            // Subtracting logic to account for padding/scrollbars
            hbox.prefWidthProperty().bind(widthProperty().subtract(20));

            setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && getItem() != null) {
                    timerService.setActiveTask(getItem());
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
                                        boolean showStatus = newStatus != TaskStatus.NONE;
                                        statusLabel.setText(showStatus ? newStatus.name() : "");
                                        statusLabel.setVisible(showStatus);
                                        statusLabel.setManaged(showStatus);
                                        taskListView.refresh();
                                    });
                                })
                                .exceptionally(ex -> {
                                    javafx.application.Platform.runLater(() -> {
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
                boolean showStatus = item.getStatus() != TaskStatus.NONE;
                statusLabel.setText(showStatus ? item.getStatus().name() : "");
                statusLabel.setVisible(showStatus);
                statusLabel.setManaged(showStatus);
                boolean hasUrl = item.getJiraUrl() != null && !item.getJiraUrl().isBlank();
                webButton.setVisible(hasUrl);
                webButton.setManaged(hasUrl);
                boolean hasSlack = item.getSlackUrl() != null && !item.getSlackUrl().isBlank();
                slackButton.setVisible(hasSlack);
                slackButton.setManaged(hasSlack);
                setGraphic(hbox);
            }
        }
    }
}