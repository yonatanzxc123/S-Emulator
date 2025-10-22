package ui.runner.components.center_right;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import ui.net.ApiClient;
import ui.runner.SelectedProgram;
import ui.runner.components.input_table.InputTableController;

public class CenterRightController {
    @FXML
    private Button newRunBtn;
    @FXML private InputTableController inputTableController;

    @FXML
    private void initialize() {
        newRunBtn.setOnAction(e -> onNewRun());
    }

    @FXML
    private void onNewRun() {
        String program = SelectedProgram.get();
        if (program == null || program.isBlank()) return;
        new Thread(() -> {
            try {
                var inputs = ApiClient.get().fetchInputsForProgram(program);
                javafx.application.Platform.runLater(() -> {
                    inputTableController.setInputs(inputs);
                });
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, "fetch-inputs").start();
    }
}
