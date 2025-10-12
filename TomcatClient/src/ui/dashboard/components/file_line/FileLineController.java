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
            showError("Failed reading file: " + e.getMessage());
            return;
        }

        if (status != null) status.setText("Uploading " + file.getName() + "...");
        if (loadButton != null) loadButton.setDisable(true);

        Thread t = new Thread(() -> {
            try {
                var api = ApiClient.get();
                var res = api.uploadProgram(xml);
                Platform.runLater(() -> {
                    if (res.ok) {
                        if (programTable != null) {
                            programTable.addProgram(res.programName, res.owner, res.instrDeg0, res.maxDegree);
                        }
                        if (functionTable != null) {
                            functionTable.addFunctions(res.programName, res.owner, res.instrDeg0, res.maxDegree, res.functions);
                        }
                        if (status != null) status.setText("Uploaded: " + res.programName + " (" + res.functions.size() + " functions)");
                    } else {
                        showError(res.error != null ? res.error : "Upload failed");
                        if (status != null) status.setText("Upload failed");
                    }
                    if (loadButton != null) loadButton.setDisable(false);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    showError("Network error: " + ex.getMessage());
                    if (status != null) status.setText("Upload failed");
                    if (loadButton != null) loadButton.setDisable(false);
                });
            }
        }, "upload-xml");
        t.setDaemon(true);
        t.start();
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Upload failed");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.show();
    }
}
