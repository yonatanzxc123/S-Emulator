package ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import system.api.EmulatorEngine;
import system.core.EmulatorEngineImpl;

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
        stage.setScene(new Scene(root, 600, 400));
        stage.show();

    }
}
