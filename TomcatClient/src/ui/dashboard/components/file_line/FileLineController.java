// java
package ui.dashboard.components.file_line;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import ui.AppContext;
import ui.dashboard.components.function_table.FunctionTableController;
import ui.dashboard.components.program_table.ProgramTableController;
import ui.net.ApiClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class FileLineController {
    @FXML private Button loadButton;
    @FXML private Label status;
    @FXML private TextField creditsTextBox;

    private AppContext ctx;
    private ProgramTableController programTable;
    private FunctionTableController functionTable;

    public void init(AppContext ctx, ProgramTableController programTable, FunctionTableController functionTable) {
        this.ctx = ctx;
        this.programTable = programTable;
        this.functionTable = functionTable;
        if (status != null) status.setText("No file loaded");
        if (loadButton != null) loadButton.setText("Load File");
    }

    @FXML
    private void onLoadFile(ActionEvent event) {
        Window w = (loadButton != null && loadButton.getScene() != null) ? loadButton.getScene().getWindow() : null;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select program XML");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("XML Files", "*.xml"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        var file = chooser.showOpenDialog(w);
        if (file == null) return;

        String xml;
        try {
            xml = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            showError("Read failed", e.getMessage());
            return;
        }

        if (status != null) status.setText("Uploading " + file.getName() + "...");
        if (loadButton != null) loadButton.setDisable(true);

        Thread t = new Thread(() -> {
            try {
                ApiClient.ProgramInfo res = ctx.api().uploadProgram(xml);
                Platform.runLater(() -> {
                    if (programTable != null) {
                        programTable.addProgram(res.name, res.owner, res.instrDeg0, res.maxDegree);
                    }
                    if (functionTable != null && res.functions != null && !res.functions.isEmpty()) {
                        functionTable.addFunctions(res.name, res.owner, res.functions);
                    }
                    if (status != null) status.setText("Uploaded " + res.name + " by " + res.owner);
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Upload failed", e.getMessage()));
            } finally {
                Platform.runLater(() -> { if (loadButton != null) loadButton.setDisable(false); });
            }
        }, "upload-program");
        t.setDaemon(true);
        t.start();
    }

    private void showError(String header, String msg) {
        if (status != null) status.setText("Error: " + header);
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText(header);
        a.setContentText(msg == null ? "" : msg);
        a.showAndWait();
    }
}
