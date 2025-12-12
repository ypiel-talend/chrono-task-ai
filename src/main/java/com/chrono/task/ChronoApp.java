package com.chrono.task;

import com.chrono.task.controller.MainController;
import com.chrono.task.persistence.JsonStorageService;
import com.chrono.task.service.TaskService;
import com.chrono.task.service.TimerService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ChronoApp extends Application {

    private TaskService taskService;
    private TimerService timerService;

    @Override
    public void start(Stage stage) throws IOException {
        // 1. Initialize Services
        var storageService = new JsonStorageService("data.json");
        taskService = new TaskService(storageService);
        taskService.init();
        
        timerService = new TimerService();

        // 2. Setup Loader with Controller Factory
        FXMLLoader loader = new FXMLLoader(ChronoApp.class.getResource("view/main_view.fxml"));
        loader.setControllerFactory(param -> new MainController(taskService, timerService));

        // 3. Show UI
        Scene scene = new Scene(loader.load(), 1000, 700);
        stage.setScene(scene);
        stage.setTitle("Chrono Task AI");
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        if (taskService != null) taskService.shutdown();
        if (timerService != null) timerService.shutdown();
    }

    public static void main(String[] args) {
        launch();
    }
}
