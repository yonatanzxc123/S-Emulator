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
    @FXML private Button chargeCreditsBtn;
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
            showError("Read Error", "Could not read file: " + e.getMessage());
            return;
        }

        if (status != null) status.setText("Uploading " + file.getName() + "...");
        if (loadButton != null) loadButton.setDisable(true);

        Thread t = new Thread(() -> {
            try {
                ApiClient.ProgramInfo p = ctx.api().uploadProgram(xml);
                Platform.runLater(() -> {
                    if (status != null) status.setText("Loaded " + file.getName());
                    if (loadButton != null) loadButton.setDisable(false);
                    if (programTable != null) programTable.addProgram(p.name, p.owner, p.instrDeg0, p.maxDegree);
                    if (functionTable != null && p.functions != null && !p.functions.isEmpty()) {
                        functionTable.addFunctions(p.name, p.owner, p.functions);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (loadButton != null) loadButton.setDisable(false);
                    showError("Upload failed", e.getMessage());
                });
            }
        }, "upload-program");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void onChargeCredits() {
        String text = creditsTextBox.getText();
        if (text == null || text.isBlank()) {
            showError("Invalid Amount", "Please enter a number of credits to add.");
            return;
        }
        long amount;
        try {
            amount = Long.parseLong(text);
        } catch (NumberFormatException e) {
            showError("Invalid Amount", "Please enter a valid number.");
            return;
        }

        if (amount <= 0) {
            showError("Invalid Amount", "Amount must be positive.");
            return;
        }

        chargeCreditsBtn.setDisable(true);
        Thread t = new Thread(() -> {
            try {
                long newTotal = ctx.api().addCredits(amount);
                Platform.runLater(() -> {
                    ctx.setCredits(newTotal);
                    creditsTextBox.clear();
                    chargeCreditsBtn.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Failed to add credits", e.getMessage());
                    chargeCreditsBtn.setDisable(false);
                });
            }
        }, "add-credits");
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
