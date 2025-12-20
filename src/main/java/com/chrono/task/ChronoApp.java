package com.chrono.task;

import com.chrono.task.controller.MainController;
import com.chrono.task.persistence.JsonStorageService;
import com.chrono.task.service.GitBackupService;
import com.chrono.task.service.GitService;
import com.chrono.task.service.TaskService;
import com.chrono.task.service.TimerService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class ChronoApp extends Application {

    private TaskService taskService;
    private TimerService timerService;
    private GitBackupService gitBackupService;
    private com.chrono.task.service.NotificationService notificationService;

    @Override
    public void start(Stage stage) throws IOException {
        // 1. Setup Settings first
        var settingsService = new com.chrono.task.persistence.SettingsStorageService();
        com.chrono.task.model.Settings settings = settingsService.load();

        // 2. Resolve Data Path
        String dataPath = settings.getDataStoragePath();
        if (dataPath == null || dataPath.isBlank()) {
            dataPath = System.getProperty("user.home") + File.separator + ".chrono-task-ai";
            settings.setDataStoragePath(dataPath);
        }
        File dataDir = new File(dataPath);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        String dataFilePath = new File(dataDir, "data.json").getAbsolutePath();

        // 3. Initialize Core Services
        var storageService = new JsonStorageService(dataFilePath);
        taskService = new TaskService(storageService);
        taskService.init();

        timerService = new TimerService();

        // 4. Initialize Git Backup
        notificationService = new com.chrono.task.service.NotificationService();
        var gitService = new GitService();
        gitBackupService = new GitBackupService(gitService, settings, notificationService);
        gitBackupService.start();

        var jiraService = new com.chrono.task.service.JiraService();

        // 5. Setup Loader with Controller Factory
        FXMLLoader loader = new FXMLLoader(ChronoApp.class.getResource("view/main_view.fxml"));
        loader.setControllerFactory(
                param -> new MainController(taskService, timerService, settingsService, settings, getHostServices(),
                        jiraService, gitBackupService));

        // 6. Show UI
        Scene scene = new Scene(loader.load(), 1000, 700);
        stage.setScene(scene);
        stage.setTitle("Chrono Task AI");
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        if (taskService != null)
            taskService.shutdown();
        if (timerService != null)
            timerService.shutdown();
        if (gitBackupService != null)
            gitBackupService.stop();
        if (notificationService != null)
            notificationService.shutdown();
    }

    public static void main(String[] args) {
        launch();
    }
}
