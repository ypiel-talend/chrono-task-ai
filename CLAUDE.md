# CLAUDE.md — chrono-task-ai

JavaFX desktop app: daily task list + per-day time tracking + markdown notes + Jira sync + git backup of data.
Single Maven module, JPMS module `com.chrono.task`. ~3k LOC. Commit messages are prefixed `[AI]`, `[Human]`, `[GH Copilot]`… — use `[AI]`.

## Build / run / test
- Toolchain: JDK 27 (sdkman), Maven 3.9. `maven.compiler.release=27`, **`--enable-preview` everywhere** (compiler, javafx:run, surefire).
- `mvn clean verify` — build + tests. `mvn test -Dtest=TaskServiceTest` — single test.
- `mvn javafx:run` — launch the app (main class `com.chrono.task.ChronoApp`, uses instance-main style `static void main()`).
- Use a throwaway settings file to avoid touching real data: `-Dchronotaskai.settings.file=/tmp/x.json` — must be enabled via the commented `<option>` in the pom's javafx-maven-plugin config; passing `-D…` to `mvn javafx:run` does **not** reach the forked JVM (real settings/data get used).
- Logs: console + `logs/chrono-task.log` (relative to CWD); `com.chrono.task` at DEBUG.
- TerminalFX is **not on Maven Central**: the pom declares the `terminalfx` repository (`https://github.com/javaterminal/TerminalFX/raw/master/releases`).
- `javafx:run` options include `--enable-native-access=javafx.graphics,javafx.web,com.sun.jna` (JDK 27 warns otherwise).
- JPMS gotcha: pty4j is an automatic module needing Kotlin; `module-info` has `requires kotlin.stdlib;` or the shell fails with `ClassNotFoundException: kotlin.jvm.internal.Intrinsics`.

## Layout (src/main/java/com/chrono/task)
| Path | Role |
|---|---|
| `ChronoApp` | Composition root: loads Settings, resolves data dir, constructs all services by hand (no DI), injects them into `MainController` via FXML controller factory; `stop()` shuts everything down (force-save, final git backup). |
| `model/Task` | Core entity (Lombok `@Data @Builder`). `taskHistory: Map<LocalDate, TaskDailyWork>`; helpers `addTime/setTime/getTimeForDate/getDurationToday/getDurationLast30Days/getTotalTime`, daily notes, labels (`getLabel/getDisplayLabel/getHistoryLabel`), `cleanupHistory()` (drops past days < 2 min with no note). `@JsonIgnore` on computed getters. `tag="new_auto"` marks JQL-created tasks. |
| `model/TaskDailyWork` | duration, note, status per day. |
| `model/TaskStatus` | TODO, IN_PROGRESS, VALIDATION, DONE, NONE, UNKNOWN, TO_DELETE. `NONE` = hide badge, not refreshed from Jira. `TO_DELETE` = excluded on save (soft delete). |
| `model/Settings` | Jira creds/base URL/JQL/refresh interval, data path, git backup interval, markdown font, `uiTheme` (DARK/LIGHT), `terminal*` (shell, start dir, theme, font, size, scrollback, copy/paste flags). |
| `model/DataStore` | `{ tasks: [...] }` root of `data.json`. |
| `persistence/JsonStorageService` | Jackson + JavaTimeModule ↔ `<dataDir>/data.json`. Implements `StorageService` (interface, mockable). |
| `persistence/SettingsStorageService` | `~/.chrono-task-ai.settings.json` (override by sysprop `chronotaskai.settings.file`). Plain mapper (ChronoUnit as string). |
| `service/TaskService` | Owns `ObservableList<Task> tasks` (single source of truth). Create (unique description), update description/Jira URL (uniqueness → `IllegalArgumentException`), `filter(query, showDone)`, `updateOrder`, auto-save every 3 min (first after 30 min!) + save on shutdown. |
| `service/TimerService` | One active task; 1 s ticker adds elapsed time into `task.addTime(today, …)` directly on the model (off FX thread). pause/resume. |
| `service/JiraService` | `java.net.http` async client, Basic auth email:token. `fetchIssue(url)` (URL regex `https://X.atlassian.net/browse/KEY`), `searchByJql` (`/rest/api/3/search/jql`, max 100), `mapStatus` (hard-coded Jira→TaskStatus mapping), `buildIssueUrl`. |
| `service/JiraRefreshService` | Scheduled: refresh status of non-DONE/NONE/TO_DELETE Jira tasks; run JQL and insert new tasks at top with `tag=new_auto` (skips rejected/closed/done/final check/eap). Exposes `isRefreshingProperty`. |
| `service/GitService` / `GitBackupService` | Shells out to `git` in data dir: init, `add data.json`, commit "Backup <ts>". Scheduled per settings; `lastCommitMessageProperty`. |
| `service/NotificationService` | AWT SystemTray notifications (fallback stderr). |
| `controller/MainController` | **~1200-line god class**: all UI logic for the 3 tabs + status bar, markdown rendering, history report, drag & drop list cell (`TaskListCell` inner class), dialogs (`showPopup`). |
| `ui/ThemeManager` | Look & feel: AtlantaFX Primer dark/light as user-agent stylesheet (global, dialogs included) + `view/app.css`; root class `theme-dark`/`theme-light`; `markdownCss(dark, font)` builds the WebView preview CSS. `style(Dialog)` for dialogs. |
| `controller/TerminalPanel` | Bottom terminal panel (Ctrl+F12): `TabPane` of TerminalFX `TerminalView` + `TerminalSession(LocalShell)` per tab, "+"/"✕" toolbar, clipboard wiring (`onCopy`/`onPasteRequested`), `applySettings()` (live look), `closeAll()`. Static `buildLook`/`buildShellSpec` map `Settings` → TerminalFX (unit tested). |
| `resources/com/chrono/task/view/app.css` | App styling on top of AtlantaFX; only AtlantaFX looked-up colors (`-color-*`) so one file serves both themes. Classes: `header-bar`, `timer-*`, `task-list` (+ cell `new-auto`/`task-active`), `status-pill status-<status>`, `icon-button`, `card`, `section-title`, `status-bar`… |
| `resources/com/chrono/task/icons/` | App icon: `app-icon.svg` (source, renders 32–512 px) + `app-icon-small.svg` (bolder variant for 16/24 px) → `app-icon-<size>.png` (re-render with `inkscape X.svg --export-type=png -w N -h N -o app-icon-N.png`). Loaded into `stage.getIcons()` in `ChronoApp` and as the tray icon in `NotificationService`. GNOME (46+) ignores the window icon for the dock/Alt-Tab: run `scripts/install-linux-desktop-entry.sh` (installs `~/.local/share/applications/chrono-task-ai.desktop` with `StartupWMClass=com.chrono.task.ChronoApp` + hicolor icons). |
| `resources/com/chrono/task/view/main_view.fxml` | Whole UI: top timer bar, TabPane (Work / History / Settings), bottom status bar. `fx:id`s map 1:1 to `@FXML` fields in MainController. |
| `module-info.java` | Must `opens` packages to javafx.fxml / jackson; add `requires` for any new library. |

## Key behaviours / data flow
- **Locate active task**: target-icon button (SVGPath; emoji don't render in JavaFX on Linux) in the header → `onLocateActiveTask` (switches to Work tab, clears filter / shows done tasks if hidden, selects + scrolls to it).
- **Window title**: live, set from `updateTimerLabel()` via static `MainController.buildWindowTitle` (unit tested): `▶ HH:MM · <task> — Chrono Task AI`, `⏸ Paused · …`, or `Chrono Task AI · Day XXhYYm` when idle; only re-set when the text changes.
- **Start timer**: double-click a task cell → `timerService.setActiveTask` (also clears `new_auto` tag). Single click only selects/loads details.
- **Editing**: text-field listeners write straight into the selected `Task` (no save button); persistence is by auto-save. Description field pasted with a Jira URL → fetch issue, replace description with summary, set jiraUrl/status/isJira.
- **List view**: `applyFilters()` replaces `taskListView` items with a *filtered copy* of `taskService.getTasks()`; reorder (DnD) mutates the master list then re-applies filters. Beware index mismatch between filtered view and master list.
- **Markdown preview**: WebView re-rendered every 3 s (flexmark) from `markdownContent` + "Daily Notes" section; scroll position preserved; edit icons call JS `editDailyNote(date)` → `alert('edit-daily-note:<date>')` → `onAlert` handler opens edit dialog.
- **History tab**: single day (text list, >2 min or note) or date range (tab-separated report; Jira tasks re-fetched for type/status).
- **Threading**: background threads (timer, autosave, Jira, git) all daemon; UI updates go through `Platform.runLater`. Timer and autosave touch `Task` objects without synchronization.
- **Theme**: `ChronoApp` applies `settings.uiTheme` before loading FXML; the Settings > Appearance combo switches live (`MainController.applyTheme`), persisted on Save.
- **Settings save** (`onSaveSettings`) persists and restarts git backup + Jira refresh services, and re-applies the terminal look to open tabs.
- **Terminal (Ctrl+F12)**: scene-level `KeyEvent` filter in `ChronoApp` (works even when a WebView has focus; the `TerminalView` also claims Ctrl+F12) → `MainController.toggleTerminal()` adds/removes the lazily created `TerminalPanel` as 2nd item of `mainSplitPane` (center is a vertical SplitPane). Hiding keeps shells alive; last tab closed/`exit` hides the panel; `ChronoApp.stop()` → `MainController.shutdown()` → `closeAll()`.

## Data files
- `~/.chrono-task-ai.settings.json` — settings (contains Jira API token in clear).
- `<dataStoragePath>/data.json` (default `~/.chrono-task-ai/`) — tasks; optional git repo there.

## Tests (src/test/java, JUnit 5 + Mockito)
`TaskTest`, `TaskServiceTest` (mocked StorageService), `TimerServiceTest`, `JiraServiceTest` (URL regex only), `JsonStorageServiceTest`, `TerminalPanelSettingsTest` (settings → TerminalFX mapping, no FX toolkit), `ThemeManagerTest`. No UI tests. Surefire argLine adds `--add-reads/--add-opens` for `com.chrono.task`, but tests actually run on the classpath (surefire prints "Unknown module: com.chrono.task"), so those flags are no-ops. `mvn clean verify` passes on JDK 27; the many `sun.misc.Unsafe` / dynamic-agent warnings (Lombok, Mockito's byte-buddy) are expected noise.

## Conventions
- Lombok for models (`@Data @Builder @NoArgsConstructor @AllArgsConstructor`, `@Builder.Default` for initialized fields); `@Slf4j` for logging (some legacy `System.out/err` remain).
- Java sources and FXML use **CRLF** line endings (pom, logback, docs are LF) — keep them when editing.
- Unnamed lambda params `_` (Java 22+). Many fully-qualified class names inline instead of imports — existing style, not required for new code.
- Validation errors surface via `IllegalArgumentException` from TaskService → caught in controller → `showPopup`.
- New persisted field on `Task`: just add it (class has `@JsonIgnoreProperties(ignoreUnknown = true)`); computed getters need `@JsonIgnore`.
- Styling: no inline `style=`/`textFill`/`<font>`; use `styleClass` + rules in `app.css` with AtlantaFX variables (also `accent`, `flat`… AtlantaFX classes).
- New UI control: add to FXML with `fx:id` + `@FXML` field / `onAction="#method"` in MainController.

## Known weak spots / improvement candidates
- MainController god class → split per tab (WorkController, HistoryController, SettingsController) or extract helpers (markdown renderer, history report builder, clipboard).
- First autosave delay is 30 min (`scheduleAtFixedRate(…, 30, 3, MINUTES)`) — likely meant 3.
- Thread-safety: TimerService ticks mutate `taskHistory` (HashMap) concurrently with autosave serialization and FX reads.
- `onAdjustTime` *sets* today's time to N minutes (README says "adjust ±").
- Markdown preview re-renders every 3 s even when nothing changed.
- Jira status mapping and JQL excluded statuses are hard-coded (company-specific).
- `totalTimerFormat` passes 3 args to a 2-placeholder format (seconds ignored).
- README outdated (Java 25, single-click to start timer).
- Settings token stored in plain JSON.
- No CI; test coverage limited to model/services.
