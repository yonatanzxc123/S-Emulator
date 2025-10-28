// java
package ui.runner.components.center_right;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import ui.AppContext;
import ui.net.ApiClient;
import ui.runner.SelectedProgram;
import ui.runner.components.input_table.InputTableController;
import ui.runner.components.var_table.VarTableController;

import java.io.IOException;
import java.util.List;

public class CenterRightController {
    @FXML private Button newRunBtn;
    @FXML private InputTableController inputTableController;
    @FXML private Button startBtn;
    @FXML private Label cyclesLbl;
    @FXML private Button dashboardBtn;
    @FXML private VarTableController varTableController;
    @FXML private ChoiceBox<String> architectureChoiceBox;

    public InputTableController getInputTableController() {
        return inputTableController;
    }

    private AppContext ctx;

    public void setAppContext(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        architectureChoiceBox.getItems().setAll( "I", "II", "III", "IV");
        architectureChoiceBox.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> highlightByArch(selected));
    }

    @FXML
    public void onNewRun() throws IOException, InterruptedException {
        String program = SelectedProgram.get();
        boolean isMainProgram = ui.ClientApp.get().getRunScreenController().getIsMainProgram();

        if (!isMainProgram) {
            var functions = ApiClient.get().listAllFunctions();
            for (var f : functions) {
                if (f.name.equals(program)) {
                    program = f.program; // Use parent program name
                    break;
                }
            }
        }

        final String inputProgram = program;
        if (program == null || program.isBlank()) return;
        new Thread(() -> {
            try {
                var inputs = ApiClient.get().fetchInputsForProgram(inputProgram);
                Platform.runLater(() -> inputTableController.setInputs(inputs));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, "fetch-inputs").start();
    }

    @FXML
    private void onStart() throws IOException, InterruptedException {
        String program = SelectedProgram.get();
        boolean isMainProgram = ui.ClientApp.get().getRunScreenController().getIsMainProgram();

        if (!isMainProgram) {
            var functions = ApiClient.get().listAllFunctions();
            for (var f : functions) {
                if (f.name.equals(program)) {
                    program = f.program; // Use parent program name
                    break;
                }
            }
        }

        if (program == null || program.isBlank()) return;
        int degree = SelectedProgram.getSelectedDegree();
        List<Long> inputs = inputTableController.getInputValues();

        final String runProgram = program;
        String selectedArch = architectureChoiceBox.getValue();

        new Thread(() -> {
            try {
                ApiClient.RunResult result = ApiClient.get().runStart(runProgram, degree, inputs, isMainProgram,selectedArch);
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

    @FXML
    private void onBackToDashboard() throws Exception {
        ui.ClientApp.get().showDashboard();
    }

    private void showError(String header, String msg) {
        javafx.scene.control.Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText(header);
        a.setContentText(msg == null ? "" : msg);
        a.showAndWait();
    }

    private void showChargeCreditsPopup() {
        javafx.scene.control.TextInputDialog dialog = new TextInputDialog();
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

    private void highlightByArch(String selectedArch) {
        int selectedTier = archTier(selectedArch);

        // Get instruction table via CenterController -> CenterLeftController
        var centerController = ((ui.runner.MainRunScreenController)
                ui.ClientApp.get().getRunScreenController()).getCenterController();
        var instructionTableController = centerController.getCenterLeftController().getInstructionTableController();
        var instructionTable = instructionTableController.getTable();

        instructionTable.setRowFactory(tv -> new javafx.scene.control.TableRow<ui.net.ApiClient.ProgramInstruction>() {
            @Override
            protected void updateItem(ui.net.ApiClient.ProgramInstruction item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else {
                    int itemTier = archTier(item.getLevel());
                    setStyle(itemTier > selectedTier && selectedTier > 0 ? "-fx-background-color: #ffcccc;" : "");
                }
            }
        });
        instructionTable.refresh();
    }

    private int archTier(String arch) {
        return switch (arch) {
            case "I" -> 1;
            case "II" -> 2;
            case "III" -> 3;
            case "IV" -> 4;
            default -> 0;
        };
    }
}
