package ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainScreen extends Application {
    @Override
    public void start(Stage stage) throws Exception {
         FXMLLoader loader = new FXMLLoader(getClass().getResource("MainScreen.fxml"));
         Parent root = loader.load();

        stage.setScene(new Scene(root, 600, 400));
        stage.show();

    }
}
