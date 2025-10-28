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
    @FXML private Button debugBtn;
    @FXML private Button stopBtn;
    @FXML private Button resumeBtn;
    @FXML private Button stepOverBtn;
    @FXML private Label cyclesLbl;
    @FXML private Button dashboardBtn;
    @FXML private VarTableController varTableController;
    @FXML private ChoiceBox<String> architectureChoiceBox;

    private String debugSessionId = null;
    private int currentPc = -1;

    public InputTableController getInputTableController() {
        return inputTableController;
    }

    public ChoiceBox<String> getArchitectureChoiceBox() {
        return architectureChoiceBox;
    }

    private AppContext ctx;

    public void setAppContext(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        architectureChoiceBox.getItems().setAll("Choose Arch" ,"I", "II", "III", "IV");
        architectureChoiceBox.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> highlightByArch(selected));
        startBtn.setDisable(true);
        debugBtn.setDisable(true);
        stopBtn.setDisable(true);
        resumeBtn.setDisable(true);
        stepOverBtn.setDisable(true);
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

        Platform.runLater(() -> {
            startBtn.setDisable(false);
            debugBtn.setDisable(false);
            stopBtn.setDisable(true);
            resumeBtn.setDisable(true);
            stepOverBtn.setDisable(true);
        });
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

        // Find highest tier in displayed instructions
        int highestTier = 0;
        for (var item : instructionTable.getItems()) {
            int itemTier = archTier(item.getLevel());
            if (itemTier > highestTier) highestTier = itemTier;
        }

        // Disable Start/Debug if selected arch is lower than required
        boolean archOk = selectedTier >= highestTier && selectedTier > 0;
        startBtn.setDisable(!archOk);
        debugBtn.setDisable(!archOk);

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

    @FXML
    private void onDebug() throws IOException, InterruptedException {
        String program = SelectedProgram.get();
        int degree = SelectedProgram.getSelectedDegree();
        List<Long> inputs = inputTableController.getInputValues();
        String arch = architectureChoiceBox.getValue();

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

        final String debugProgram = program;
        new Thread(() -> {
            try {
                ApiClient.DebugState state = ctx.api().debugStart(debugProgram, degree, arch, inputs, isMainProgram);
                if ("insufficient_credits".equals(state.error)) {
                    Platform.runLater(this::showChargeCreditsPopup);
                    return;
                }
                if (!state.ok) {
                    Platform.runLater(() -> showError("Debug start failed", state.error));
                    return;
                }
                debugSessionId = state.id;
                ApiClient.DebugState firstStep = ctx.api().debugStep(debugSessionId);
                Platform.runLater(() -> {
                    handleDebugResponse(firstStep);
                    stopBtn.setDisable(false);
                    resumeBtn.setDisable(false);
                    stepOverBtn.setDisable(false);
                    startBtn.setDisable(true);
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Debug start failed", e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void onStepOverBtn() {
        if (debugSessionId == null) return;
        new Thread(() -> {
            try {
                ApiClient.DebugState state = ctx.api().debugStep(debugSessionId);
                handleDebugResponse(state);
            } catch (Exception e) {
                Platform.runLater(() -> showError("Step failed", e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void onResumeBtn() {
        if (debugSessionId == null) return;
        new Thread(() -> {
            try {
                ApiClient.DebugState state = ctx.api().debugResume(debugSessionId);
                handleDebugResponse(state);
            } catch (Exception e) {
                Platform.runLater(() -> showError("Resume failed", e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void onStopBtn() {
        if (debugSessionId == null) return;
        new Thread(() -> {
            try {
                ctx.api().debugStop(debugSessionId,ui.ClientApp.get().getRunScreenController().getIsMainProgram());
                debugSessionId = null;
                Platform.runLater(this::endDebugMode);
            } catch (Exception e) {
                Platform.runLater(() -> showError("Stop failed", e.getMessage()));
            }
        }).start();
    }

    private void fetchDebugState() {
        if (debugSessionId == null) return;
        new Thread(() -> {
            try {
                ApiClient.DebugState state = ctx.api().debugState(debugSessionId);
                handleDebugResponse(state);
            } catch (Exception e) {
                Platform.runLater(() -> showError("Fetch state failed", e.getMessage()));
            }
        }).start();
    }

    private void handleDebugResponse(ApiClient.DebugState state) {
        if ("insufficient_credits".equals(state.error) || "credit_exhausted".equals(state.error)) {
            Platform.runLater(this::showChargeCreditsPopup);
            endDebugMode();
            return;
        }
        if (!state.ok) {
            Platform.runLater(() -> showError("Debug failed", state.error));
            endDebugMode();
            return;
        }
        currentPc = state.pc;
        Platform.runLater(() -> {
            highlightCurrentLine(currentPc);
            cyclesLbl.setText("Cycles: " + state.cycles);
            varTableController.setVars(state.vars);
            if (ctx != null) ctx.setCredits(state.creditsLeft);
            // If halted, treat as program end
            if (state.halted) {
                try {
                    ctx.api().debugStop(debugSessionId, ui.ClientApp.get().getRunScreenController().getIsMainProgram());
                    debugSessionId = null;
                    Platform.runLater(() -> {
                        addDebugRunToHistory(state);
                        endDebugMode();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void disableRunDebugButtons() {
        startBtn.setDisable(true);
    }


    private void highlightCurrentLine(int pc) {
        var centerController = ((ui.runner.MainRunScreenController)
                ui.ClientApp.get().getRunScreenController()).getCenterController();
        var instructionTableController = centerController.getCenterLeftController().getInstructionTableController();
        var instructionTable = instructionTableController.getTable();

        instructionTable.setRowFactory(tv -> new TableRow<ui.net.ApiClient.ProgramInstruction>() {
            @Override
            protected void updateItem(ui.net.ApiClient.ProgramInstruction item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else {
                    setStyle(item.getIndex() == pc ? "-fx-background-color: #cceeff;" : "");
                }
            }
        });
        instructionTable.refresh();
    }

    private void endDebugMode() {
        highlightCurrentLine(-1);
        stopBtn.setDisable(true);
        resumeBtn.setDisable(true);
        stepOverBtn.setDisable(true);
        startBtn.setDisable(false);
        debugBtn.setDisable(false);
    }

    private void addDebugRunToHistory(ApiClient.DebugState state) {
        new Thread(() -> {
            try {
                var history = ApiClient.get().fetchOwnRunHistory();
                Platform.runLater(() -> {
                    // Update history table UI here
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
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
