package ui.components.header;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.nio.file.Path;



public class HeaderController {
    @FXML
    private Button loadFileBtn;

    @FXML
    private Label loadFileLbl;

    private SimpleStringProperty loadFileLblProp = new SimpleStringProperty("No File Loaded");
    private SimpleBooleanProperty isLoadedProp = new SimpleBooleanProperty(false);

    @FXML
    private void initialize() {
        loadFileLbl.textProperty().bind(loadFileLblProp);
        loadFileLbl.prefHeightProperty().bind(loadFileBtn.heightProperty());
    }

    @FXML
    public void onActionLoadFile() {
        Stage stage = (Stage) loadFileBtn.getScene().getWindow();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open XML File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));
        File file = fileChooser.showOpenDialog(stage);
        if (file == null) {
           return;
        }
        Task<Path> task = new Task<Path>() {
            @Override
            protected Path call() throws Exception {
                updateMessage("Loading " + file.getAbsolutePath() + "...");
                final int steps = 40;
                for (int i = 1; i <= steps; i++) {
                    if (isLoadedProp.get()) {break;}
                    Thread.sleep(50);
                    updateProgress(i, steps);
                }
                updateMessage("Loaded");
                return file.toPath();
            }
        };
        String absolutePath = file.getAbsolutePath();
        loadFileLblProp.set(absolutePath);

        Stage dialog = createLoadingDialog(stage,task);

        task.setOnSucceeded(e -> {
            loadFileLblProp.unbind();
            loadFileLblProp.set(task.getValue().toAbsolutePath().toString());
            dialog.close();
        });
        task.setOnFailed(e -> {
            loadFileLblProp.unbind();
            loadFileLblProp.set("Failed: " + task.getException().getMessage());
            dialog.close();
        });
        task.setOnCancelled(e -> {
            loadFileLblProp.unbind();
            loadFileLblProp.set("Cancelled");
            dialog.close();
        });

        loadFileLblProp.bind(task.messageProperty());

        dialog.setOnShown(e -> {
            Thread t = new Thread(task, "file-loader");
            t.setDaemon(true);
            t.start();
        });
        dialog.showAndWait();

    }
    private Stage createLoadingDialog(Stage owner, Task<?> task) {
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.progressProperty().bind(task.progressProperty());

        Label msg = new Label();
        msg.textProperty().bind(task.messageProperty());

        ProgressBar bar = new ProgressBar();
        bar.setPrefWidth(240);
        bar.progressProperty().bind(task.progressProperty());

        HBox header = new HBox(10, spinner, msg);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(12, header, bar);
        VBox.setMargin(bar, new Insets(0, 0, 0, 34));
        root.setPadding(new Insets(14));
        root.setFillWidth(true);

        Stage dlg = new Stage(StageStyle.UTILITY);
        dlg.initOwner(owner);
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.setResizable(false);
        dlg.setTitle("Loading");
        dlg.setScene(new Scene(root));
        return dlg;
    }
}
