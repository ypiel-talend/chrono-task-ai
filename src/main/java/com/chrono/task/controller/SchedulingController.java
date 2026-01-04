package com.chrono.task.controller;

import com.chrono.task.model.ScheduledReminder;
import com.chrono.task.service.SchedulingService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

@Slf4j
public class SchedulingController {

    private final SchedulingService schedulingService;

    @FXML
    private ListView<ScheduledReminder> reminderListView;
    @FXML
    private Button addReminderButton;
    @FXML
    private Button editReminderButton;
    @FXML
    private Button deleteReminderButton;
    @FXML
    private Button toggleReminderButton;

    @FXML
    private VBox reminderFormBox;
    @FXML
    private TextField titleField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private TextField linkField;
    @FXML
    private RadioButton oneTimeRadio;
    @FXML
    private RadioButton weeklyRadio;
    @FXML
    private ToggleGroup reminderTypeGroup;

    // One Time fields
    @FXML
    private VBox oneTimeBox;
    @FXML
    private DatePicker datePicker;
    @FXML
    private Spinner<Integer> hourSpinner;
    @FXML
    private Spinner<Integer> minuteSpinner;

    // Weekly fields
    @FXML
    private VBox weeklyBox;
    @FXML
    private ComboBox<DayOfWeek> dayOfWeekComboBox;
    @FXML
    private Spinner<Integer> weeklyHourSpinner;
    @FXML
    private Spinner<Integer> weeklyMinuteSpinner;

    @FXML
    private Button saveReminderButton;
    @FXML
    private Button cancelReminderButton;

    private ScheduledReminder editingReminder = null;

    public SchedulingController(SchedulingService schedulingService) {
        this.schedulingService = schedulingService;
    }

    @FXML
    public void initialize() {
        setupListView();
        setupSpinners();
        setupComboBoxes();
        setupRadioButtons();
        setupListeners();
        refreshReminderList();
    }

    private void setupListView() {
        reminderListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(ScheduledReminder reminder, boolean empty) {
                super.updateItem(reminder, empty);
                if (empty || reminder == null) {
                    setText(null);
                } else {
                    String status = reminder.isEnabled() ? "✓" : "✗";
                    String timeInfo = "";
                    if (reminder.getType() == ScheduledReminder.ReminderType.ONE_TIME) {
                        timeInfo = reminder.getDate() + " " + reminder.getTime();
                    } else {
                        timeInfo = reminder.getDayOfWeek() + " " + reminder.getWeeklyTime();
                    }
                    setText(String.format("[%s] %s - %s", status, reminder.getTitle(), timeInfo));
                    setStyle(reminder.isEnabled() ? "" : "-fx-text-fill: gray;");
                }
            }
        });
    }

    private void setupSpinners() {
        hourSpinner = new Spinner<>(0, 23, 12);
        minuteSpinner = new Spinner<>(0, 59, 0);
        weeklyHourSpinner = new Spinner<>(0, 23, 12);
        weeklyMinuteSpinner = new Spinner<>(0, 59, 0);

        hourSpinner.setEditable(true);
        minuteSpinner.setEditable(true);
        weeklyHourSpinner.setEditable(true);
        weeklyMinuteSpinner.setEditable(true);

        // Replace the spinners in the FXML
        oneTimeBox.getChildren().stream()
            .filter(node -> node instanceof javafx.scene.layout.HBox)
            .map(node -> (javafx.scene.layout.HBox) node)
            .findFirst()
            .ifPresent(hbox -> {
                hbox.getChildren().set(0, hourSpinner);
                hbox.getChildren().set(2, minuteSpinner);
            });

        weeklyBox.getChildren().stream()
            .filter(node -> node instanceof javafx.scene.layout.HBox)
            .map(node -> (javafx.scene.layout.HBox) node)
            .findFirst()
            .ifPresent(hbox -> {
                hbox.getChildren().set(0, weeklyHourSpinner);
                hbox.getChildren().set(2, weeklyMinuteSpinner);
            });
    }

    private void setupComboBoxes() {
        dayOfWeekComboBox.getItems().addAll(DayOfWeek.values());
        dayOfWeekComboBox.getSelectionModel().selectFirst();
    }

    private void setupRadioButtons() {
        reminderTypeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == oneTimeRadio) {
                oneTimeBox.setVisible(true);
                oneTimeBox.setManaged(true);
                weeklyBox.setVisible(false);
                weeklyBox.setManaged(false);
            } else {
                oneTimeBox.setVisible(false);
                oneTimeBox.setManaged(false);
                weeklyBox.setVisible(true);
                weeklyBox.setManaged(true);
            }
        });
    }

    private void setupListeners() {
        reminderListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelection = newVal != null;
            editReminderButton.setDisable(!hasSelection);
            deleteReminderButton.setDisable(!hasSelection);
            toggleReminderButton.setDisable(!hasSelection);

            if (hasSelection) {
                toggleReminderButton.setText(newVal.isEnabled() ? "Disable" : "Enable");
            }
        });
    }

    private void refreshReminderList() {
        reminderListView.getItems().clear();
        reminderListView.getItems().addAll(schedulingService.getReminders());
    }

    @FXML
    private void onAddReminder() {
        editingReminder = null;
        clearForm();
        reminderFormBox.setVisible(true);
        reminderFormBox.setManaged(true);
        datePicker.setValue(LocalDate.now());
    }

    @FXML
    private void onEditReminder() {
        ScheduledReminder selected = reminderListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        editingReminder = selected;
        loadReminderToForm(selected);
        reminderFormBox.setVisible(true);
        reminderFormBox.setManaged(true);
    }

    @FXML
    private void onDeleteReminder() {
        ScheduledReminder selected = reminderListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Reminder");
        alert.setHeaderText("Delete this reminder?");
        alert.setContentText(selected.getTitle());

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                schedulingService.deleteReminder(selected);
                refreshReminderList();
            }
        });
    }

    @FXML
    private void onToggleReminder() {
        ScheduledReminder selected = reminderListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        selected.setEnabled(!selected.isEnabled());
        schedulingService.updateReminder(selected);
        refreshReminderList();
    }

    @FXML
    private void onSaveReminder() {
        if (titleField.getText().isEmpty()) {
            showAlert("Validation Error", "Title is required");
            return;
        }

        if (editingReminder == null) {
            // Create new reminder
            ScheduledReminder reminder = ScheduledReminder.builder()
                .title(titleField.getText())
                .description(descriptionArea.getText())
                .link(linkField.getText())
                .enabled(true)
                .build();

            if (oneTimeRadio.isSelected()) {
                if (datePicker.getValue() == null) {
                    showAlert("Validation Error", "Date is required for one-time reminders");
                    return;
                }
                reminder.setType(ScheduledReminder.ReminderType.ONE_TIME);
                reminder.setDate(datePicker.getValue());
                reminder.setTime(LocalTime.of(hourSpinner.getValue(), minuteSpinner.getValue()));
            } else {
                reminder.setType(ScheduledReminder.ReminderType.WEEKLY);
                reminder.setDayOfWeek(dayOfWeekComboBox.getValue());
                reminder.setWeeklyTime(LocalTime.of(weeklyHourSpinner.getValue(), weeklyMinuteSpinner.getValue()));
            }

            schedulingService.addReminder(reminder);
        } else {
            // Update existing reminder
            editingReminder.setTitle(titleField.getText());
            editingReminder.setDescription(descriptionArea.getText());
            editingReminder.setLink(linkField.getText());

            if (oneTimeRadio.isSelected()) {
                if (datePicker.getValue() == null) {
                    showAlert("Validation Error", "Date is required for one-time reminders");
                    return;
                }
                editingReminder.setType(ScheduledReminder.ReminderType.ONE_TIME);
                editingReminder.setDate(datePicker.getValue());
                editingReminder.setTime(LocalTime.of(hourSpinner.getValue(), minuteSpinner.getValue()));
            } else {
                editingReminder.setType(ScheduledReminder.ReminderType.WEEKLY);
                editingReminder.setDayOfWeek(dayOfWeekComboBox.getValue());
                editingReminder.setWeeklyTime(LocalTime.of(weeklyHourSpinner.getValue(), weeklyMinuteSpinner.getValue()));
            }

            schedulingService.updateReminder(editingReminder);
        }

        refreshReminderList();
        onCancelReminder();
    }

    @FXML
    private void onCancelReminder() {
        editingReminder = null;
        clearForm();
        reminderFormBox.setVisible(false);
        reminderFormBox.setManaged(false);
    }

    private void clearForm() {
        titleField.clear();
        descriptionArea.clear();
        linkField.clear();
        datePicker.setValue(LocalDate.now());
        hourSpinner.getValueFactory().setValue(12);
        minuteSpinner.getValueFactory().setValue(0);
        weeklyHourSpinner.getValueFactory().setValue(12);
        weeklyMinuteSpinner.getValueFactory().setValue(0);
        dayOfWeekComboBox.getSelectionModel().selectFirst();
        oneTimeRadio.setSelected(true);
    }

    private void loadReminderToForm(ScheduledReminder reminder) {
        titleField.setText(reminder.getTitle());
        descriptionArea.setText(reminder.getDescription());
        linkField.setText(reminder.getLink());

        if (reminder.getType() == ScheduledReminder.ReminderType.ONE_TIME) {
            oneTimeRadio.setSelected(true);
            datePicker.setValue(reminder.getDate());
            hourSpinner.getValueFactory().setValue(reminder.getTime().getHour());
            minuteSpinner.getValueFactory().setValue(reminder.getTime().getMinute());
        } else {
            weeklyRadio.setSelected(true);
            dayOfWeekComboBox.setValue(reminder.getDayOfWeek());
            weeklyHourSpinner.getValueFactory().setValue(reminder.getWeeklyTime().getHour());
            weeklyMinuteSpinner.getValueFactory().setValue(reminder.getWeeklyTime().getMinute());
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}