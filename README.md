# Chrono Task AI

A powerful Java 25 + JavaFX Application for daily task management, time tracking, and Jira integration.

The project is called `Chrono Task AI` not because it proposes AI features, but because it has been mostly developed by
Google Antigravity AI-powered integrated development environment. It is a project to test this tool.

## Features & Usage

### 1. Task Management
Organize your daily work with a flexible list of tasks.
*   **Create**: Click the **"Add New Task"** button at the bottom of the list.
*   **Edit**: Select a task and use the form on the right to update its **Description**.
*   **Reorder**: Drag and drop tasks within the list to prioritize them.
*   **Filter**: Use the search bar at the top of the list to filter tasks by name.

### 2. Smart Time Tracking
Automatically track how much time you spend on each contest.
*   **Auto-Start**: Clicking a task immediately starts the timer for it.
*   **Statistics**: The top bar displays:
    *   **Active**: Duration of the current session.
    *   **Today**: Total duration for the active task today.
    *   **30d**: Total duration for the active task over the last 30 days.
    *   **Total**: Cumulative duration since the task was created.
*   **Manual Adjustment**: Need to correct a mistake? Enter a duration in minutes (positive or negative) in the **"Adjust Time"** field and click **"Apply"**.

### 3. Jira Integration
Seamlessly link your local tasks to Jira tickets.
*   **Linking**: Paste a full Jira URL (e.g., `https://company.atlassian.net/browse/PROJ-123`) into the **Jira URL** field or the Task Description.
*   **Sync**: The app will automatically fetch the Ticket Summary and Status. The Status is displayed as a badge on the task list.
*   **Refresh**: Click the Status badge on a task to manually refresh its status from Jira.

### 4. Git Integration (Automatic Backup)
Keep your data safe with automated Git versioning.
*   **Automatic Backups**: If Git is installed and enabled, the application automatically commits your `data.json` at regular intervals.
*   **Init Repository**: The app will automatically run `git init` in your data directory if it's not already a repository.
*   **Status Tracking**: The UI displays the last commit message to confirm backups are working.

### 5. Markdown Notes & Documentation
Keep detailed context right next to your tasks.
*   **Markdown Editor**: Use the central text area to write comprehensive documentation for your task.
*   **Live Preview**: See your Markdown rendered instantly in the WebView panel.
*   **Daily Notes**: Use the **"Daily note"** field to add specific updates for *today*. These are stored efficiently in the task's history.

### 6. History & Insights
Review your past work.
*   **Calendar View**: Switch to the **History** tab and select a date from the DatePicker.
*   **Daily Log**: View a summary of all tasks worked on that day, including durations and daily notes.

### 7. Data Persistence
*   **Auto-Save**: The application automatically saves all tasks and history to `data.json` every 30 seconds.

## Configuration

Detailed settings can be configured in the **Settings** tab. The configuration is stored in `~/.chrono-task-ai.settings.json`.

### Jira Settings
- **Jira Email**: Your Atlassian account email.
- **Jira API Token**: An API token generated from your Atlassian account.

### Storage Settings
- **Data Storage Path**: The directory where `data.json` is stored. Default: `~/.chrono-task-ai/`.

### Git Backup Settings
- **Enable Git Backup**: Toggle automated Git backups on or off.
- **Backup Interval**: How often to perform a backup (e.g., every 1 hour).
- **Backup Unit**: Units for the interval (Minutes, Hours, Days).

## Architecture

This project follows a clean **MVC (Model-View-Controller)** architecture built with **JavaFX**.

### Key Components

*   **Model (`com.chrono.task.model`)**:
    *   `Task`: The core entity containing description, history (`TaskDailyWork`), and Jira metadata.
    *   `Settings`: Stores user configuration.
*   **View (`src/main/resources/.../view`)**:
    *   `main_view.fxml`: Defines the UI layout.
*   **Controller (`com.chrono.task.controller`)**:
    *   `MainController`: Handles UI events and updates the View.
*   **Services (`com.chrono.task.service`)**:
    *   `TaskService`: Manages the list of tasks and CRUD operations.
    *   `TimerService`: Background thread for time tracking.
    *   `JiraService`: REST client for Atlassian API.
    *   `GitService`: Executes Git commands.
    *   `GitBackupService`: Manages the scheduled backup tasks.
*   **Persistence (`com.chrono.task.persistence`)**:
    *   `JsonStorageService`: Manages saving/loading the main `data.json`.
    *   `SettingsStorageService`: Manages `~/.chrono-task-ai.settings.json`.

### Libraries & Tools

*   **Java 25**: Leveraging the latest language features.
*   **JavaFX**: For a responsive desktop UI.
*   **Jackson**: For robust JSON data binding.
*   **Flexmark**: For parsing and rendering Markdown.
*   **Lombok**: To reduce boilerplate code.

## Requirements

*   JDK 25
*   Maven 3.8+
*   Git (Optional, for backup features)

## Build & Run

```bash
# Build and Run Tests
mvn clean verify

# Start Application
mvn javafx:run
```
