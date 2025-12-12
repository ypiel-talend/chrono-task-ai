# Chrono Task AI

A Java 25 + JavaFX Application for daily task management and time tracking.

## Features

- **Task Management**: Create, edit, and reorder tasks (Drag'n'Drop).
- **Time Tracking**: Automatic timer when a task is selected.
- **Markdown Notes**: Per-task Markdown editor with live preview.
- **History**: View daily time logs.
- **Auto-Save**: Data persisted to `data.json` every 30 seconds.

## Requirements

- JDK 25 (Enabled Preview features recommended if using experimental features, though this project uses standard Java 25 features).
- Maven 3.8+

## Build

To compile and run tests:

```bash
mvn clean verify
```

## Run

To launch the application:

```bash
mvn javafx:run
```

## Structure

- `src/main/java/com/chrono/task/model`: Entities (`Task`, `DataStore`).
- `src/main/java/com/chrono/task/service`: Business logic (`TaskService`, `TimerService`).
- `src/main/java/com/chrono/task/persistence`: JSON Storage (`Jackson`).
- `src/main/java/com/chrono/task/controller`: JavaFX Controllers.
- `src/main/resources/com/chrono/task/view`: FXML files.

## Testing

Unit tests cover Models, Services, and Persistence logic.

```bash
mvn test
```
