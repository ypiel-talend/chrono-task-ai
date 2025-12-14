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
*   **Configuration**:
    1.  Go to the **Settings** tab.
    2.  Enter your **Jira Email** and **API Token**.
    3.  Click **"Save Settings"**.
*   **Linking**: Paste a full Jira URL (e.g., `https://company.atlassian.net/browse/PROJ-123`) into the **Jira URL** field or the Task Description.
*   **Sync**: The app will automatically fetch the Ticket Summary and Status. The Status is displayed as a badge on the task list.
*   **Refresh**: Click the Status badge on a task to manually refresh its status from Jira.

### 4. Markdown Notes & Documentation
Keep detailed context right next to your tasks.
*   **Markdown Editor**: Use the central text area to write comprehensive documentation for your task.
*   **Live Preview**: See your Markdown rendered instantly in the WebView panel.
*   **Daily Notes**: Use the **"Daily note"** field to add specific updates for *today*. These are stored efficiently in the task's history.

### 5. History & Insights
Review your past work.
*   **Calendar View**: Switch to the **History** tab and select a date from the DatePicker.
*   **Daily Log**: View a summary of all tasks worked on that day, including durations and daily notes.

### 6. Auto-Save
*   Never lose your data. The application automatically saves all tasks and history to `data.json` every 30 seconds.

## Architecture

This project follows a clean **MVC (Model-View-Controller)** architecture built with **JavaFX**.

### Key Components

*   **Model (`com.chrono.task.model`)**:
    *   `Task`: The core entity containing description, history (`TaskDailyWork`), and Jira metadata.
    *   `Settings`: Stores user configuration (Jira credentials).
*   **View (`src/main/resources/.../view`)**:
    *   `main_view.fxml`: Defines the UI layout (BorderPane, SplitPane, TabPane).
*   **Controller (`com.chrono.task.controller`)**:
    *   `MainController`: Handles UI events (clicks, edits, drag-and-drop) and updates the View.
*   **Services (`com.chrono.task.service`)**:
    *   `TaskService`: Manages the list of tasks, filtering, and CRUD operations.
    *   `TimerService`: Runs a background thread to update the active task's duration every second.
    *   `JiraService`: A REST client using `java.net.http` to communicate with the Atlassian API.
*   **Persistence (`com.chrono.task.persistence`)**:
    *   Uses **Jackson** `ObjectMapper` to serialize the entire state to `data.json`.
    *   `SettingsStorageService` manages specific user settings in `~/.chrono-task-ai.json`.

### Libraries & Tools

*   **Java 25**: Leveraging the latest language features.
*   **JavaFX**: For a responsive desktop UI.
*   **Jackson**: For robust JSON data binding.
*   **Flexmark**: For parsing and rendering Markdown.
*   **Lombok**: To reduce boilerplate code.

## Requirements

*   JDK 25
*   Maven 3.8+

## Build & Run

```bash
# Build and Run Tests
mvn clean verify

# Start Application
mvn javafx:run
```