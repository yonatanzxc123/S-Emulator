package ui;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import system.api.EmulatorEngine;
import system.core.EmulatorEngineImpl;
import ui.components.center.CenterController;
import ui.components.header.HeaderController;

public class MainScreen extends Application {

    private EmulatorEngine engine;

    @Override
    public void init() {
        engine = new EmulatorEngineImpl();  // one instance for the app lifetime
    }
    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader loader = new FXMLLoader(getClass().getResource("MainScreen.fxml"));

        loader.setControllerFactory(type -> {
            try {
                Object controller = type.getDeclaredConstructor().newInstance();
                if (controller instanceof EngineInjector aware) {
                    aware.setEngine(engine);
                }
                return controller;
            } catch (Exception e) {
                throw new RuntimeException("Failed to construct controller: " + type, e);
            }
        });
        Parent root = loader.load();
        stage.setScene(new Scene(root, 1000, 700));
        stage.show();

    }
}
