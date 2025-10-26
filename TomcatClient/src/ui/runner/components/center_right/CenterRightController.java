// java
package ui.runner.components.center_right;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import ui.AppContext;
import ui.net.ApiClient;
import ui.runner.SelectedProgram;
import ui.runner.components.input_table.InputTableController;
import ui.runner.components.var_table.VarTableController;

import java.util.List;

public class CenterRightController {
    @FXML private Button newRunBtn;
    @FXML private InputTableController inputTableController;
    @FXML private Button startBtn;
    @FXML private Label cyclesLbl;
    @FXML private VarTableController varTableController;

    private AppContext ctx;

    public void setAppContext(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {}

    @FXML
    private void onNewRun() {
        String program = SelectedProgram.get();
        if (program == null || program.isBlank()) return;
        new Thread(() -> {
            try {
                var inputs = ApiClient.get().fetchInputsForProgram(program);
                Platform.runLater(() -> inputTableController.setInputs(inputs));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, "fetch-inputs").start();
    }

    @FXML
    private void onStart() {
        String program = SelectedProgram.get();
        if (program == null || program.isBlank()) return;
        int degree = SelectedProgram.getSelectedDegree();
        List<Long> inputs = inputTableController.getInputValues();

        new Thread(() -> {
            try {
                ApiClient.RunResult result = ApiClient.get().runStart(program, degree, inputs);
                if ("insufficient_credits".equals(result.error)) {
                    Platform.runLater(this::showChargeCreditsPopup);
                    return;
                }
                if (result.error != null) {
                    Platform.runLater(() -> showError("Run failed", result.error));
                    return;
                }
                Platform.runLater(() -> {
                    cyclesLbl.setText("Cycles: " + result.cycles);
                    varTableController.setVars(result.vars);
                    if (ctx != null) ctx.setCredits(result.creditsLeft); // Update credits in context
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> showError("Run failed", ex.getMessage()));
            }
        }, "run-start").start();
    }

    private void showError(String header, String msg) {
        javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText(header);
        a.setContentText(msg == null ? "" : msg);
        a.showAndWait();
    }

    private void showChargeCreditsPopup() {
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
        dialog.setTitle("Insufficient Credits");
        dialog.setHeaderText("You do not have enough credits to run this program.");
        dialog.setContentText("Enter credits to add:");

        dialog.showAndWait().ifPresent(input -> {
            try {
                long amount = Long.parseLong(input);
                if (amount > 0) {
                    new Thread(() -> {
                        try {
                            long newTotal = ApiClient.get().addCredits(amount);
                            Platform.runLater(() -> {
                                if (ctx != null) ctx.setCredits(newTotal); // Update context so header updates
                            });
                        } catch (Exception e) {
                            Platform.runLater(() -> showError("Failed to add credits", e.getMessage()));
                        }
                    }, "add-credits").start();
                }
            } catch (NumberFormatException e) {
                showError("Invalid Amount", "Please enter a valid number.");
            }
        });
    }
}
