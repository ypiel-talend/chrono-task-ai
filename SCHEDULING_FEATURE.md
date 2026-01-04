# Scheduling Feature

## Overview
The Scheduling feature allows you to create reminders that will be displayed as system notifications at specified times. This is useful for setting up recurring meetings, deadlines, or one-time events.

## Features

### Reminder Types

1. **One-Time Reminder**
   - Schedule a notification for a specific date and time
   - Default date is today
   - Perfect for deadlines, appointments, or one-off events

2. **Weekly Reminder**
   - Schedule a recurring notification for a specific day of the week and time
   - Great for recurring meetings, weekly reviews, or regular check-ins

### Reminder Properties

Each reminder has:
- **Title** (required): The main heading shown in the notification
- **Description** (optional): Additional details about the reminder
- **Link** (optional): A URL that can be included in the notification
- **Enabled/Disabled state**: Toggle reminders on/off without deleting them

## How to Use

### Creating a Reminder

1. Navigate to the **Scheduling** tab
2. Click the **Add Reminder** button
3. Fill in the reminder details:
   - Enter a title (required)
   - Optionally add a description and link
   - Choose the reminder type (One Time or Weekly)
   - Set the date/day and time
4. Click **Save**

### Editing a Reminder

1. Select a reminder from the list
2. Click the **Edit** button
3. Modify the fields as needed
4. Click **Save**

### Deleting a Reminder

1. Select a reminder from the list
2. Click the **Delete** button
3. Confirm the deletion

### Disabling/Enabling a Reminder

1. Select a reminder from the list
2. Click the **Disable** or **Enable** button (toggles based on current state)
3. Disabled reminders are shown in gray and will not trigger notifications

## Notification Behavior

- Reminders are checked every minute
- When a reminder's time matches the current time, a system notification is displayed
- The notification includes:
  - Title: "Reminder: [Your Title]"
  - Message: Your description (if provided)
  - Link: Your link (if provided)

## Data Storage

- Reminders are stored in `scheduling.json` in your data directory
- The file is automatically saved when you add, edit, or delete reminders
- Data persists between application restarts

## Technical Details

### Components

- **Model Classes**:
  - `ScheduledReminder`: Represents a single reminder
  - `SchedulingData`: Container for all reminders
  - `ReminderType`: Enum for ONE_TIME and WEEKLY types

- **Service Layer**:
  - `SchedulingService`: Manages reminder lifecycle and checking
  - `SchedulingStorageService`: Handles persistence

- **UI Layer**:
  - `SchedulingController`: Manages the Scheduling tab UI
  - `scheduling_view.fxml`: UI layout

### Integration

The scheduling service is:
- Initialized in `ChronoApp.start()`
- Runs a background scheduler that checks reminders every minute
- Properly shut down when the application closes
- Integrated with the existing `NotificationService`

## Examples

### One-Time Reminder Example
```
Title: Project Deadline
Description: Submit the final project report
Link: https://jira.company.com/PROJECT-123
Type: One Time
Date: 2026-01-10
Time: 14:30
```

### Weekly Reminder Example
```
Title: Team Standup
Description: Daily team sync meeting
Link: https://zoom.us/j/123456789
Type: Weekly
Day: Monday
Time: 09:00
```

## Future Enhancements

Potential improvements:
- Daily reminders
- Monthly reminders
- Custom repeat intervals
- Snooze functionality
- Reminder categories/tags
- Sound notifications
- Desktop integration with calendar apps