
package ui.runner;
import javafx.application.Platform;
import javafx.fxml.FXML;
import ui.AppContext;
import ui.components.header.HeaderController;
import ui.net.ApiClient;
import ui.runner.components.center.CenterController;

import java.io.IOException;
import java.util.List;


// java
public class MainRunScreenController {
    private AppContext ctx;
    @FXML private HeaderController headerController;
    @FXML private CenterController centerController;

    private boolean isMainProgram;

    public void setMainProgram(boolean mainProgram) {
        isMainProgram = mainProgram;
    }
    public boolean getIsMainProgram() {
        return isMainProgram;
    }

    public CenterController getCenterController() {
        return centerController;
    }

    public MainRunScreenController() {}

    public MainRunScreenController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {

        if (headerController != null && ctx != null) {
            updateHeader();
        } else {
            javafx.application.Platform.runLater(this::updateHeader);
        }
        if (centerController != null && ctx != null) {
            centerController.init(ctx);
        } else {
            javafx.application.Platform.runLater(() -> {
                if (centerController != null && ctx != null) centerController.init(ctx);
            });
        }
    }

    private void updateHeader() {
        if (headerController != null && ctx != null) {
            String u = null;
            try {
                u = (ctx.api() != null) ? ctx.api().currentUser() : null;
            } catch (Exception ignore) {}
            if (u == null || u.isBlank()) u = "User";
            ctx.setUsername(u);

            headerController.bindUsername(ctx.usernameProperty());
            headerController.setTitleSuffix("Execution");
            headerController.bindCredits(ctx.creditsProperty());
        }
    }

    public void prepareRerun() throws IOException, InterruptedException {
        if (centerController != null && centerController.getCenterRightController() != null) {
            centerController.getCenterRightController().onNewRun();
            Platform.runLater(() -> {
                var rightCtrl = centerController.getCenterRightController();
                var leftCtrl = centerController.getCenterLeftController();
                if (rightCtrl != null && rightCtrl.getInputTableController() != null && leftCtrl != null) {
                    String program = ui.runner.SelectedProgram.get();
                    int degree = ui.runner.SelectedProgram.getSelectedDegree();
                    List<Long> inputs = ui.runner.SelectedProgram.getInputs();

                    new Thread(() -> {
                        try {
                            String parentProgram = program;
                            String functionName = null;
                            boolean isFunction = false;
                            var functions = ApiClient.get().listAllFunctions();
                            for (var f : functions) {
                                if (f.name.equals(program)) {
                                    parentProgram = f.program;
                                    functionName = f.name;
                                    isFunction = true;
                                    break;
                                }
                            }
                            var inputsInfo = isFunction
                                    ? ApiClient.get().fetchInputsForProgram(parentProgram, functionName)
                                    : ApiClient.get().fetchInputsForProgram(program, null);

                            Platform.runLater(() -> {
                                rightCtrl.getInputTableController().setInputs(inputsInfo);
                                rightCtrl.getInputTableController().setInputValues(inputs);
                                leftCtrl.expandToDegree(degree);
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
            });
        }
    }
}


