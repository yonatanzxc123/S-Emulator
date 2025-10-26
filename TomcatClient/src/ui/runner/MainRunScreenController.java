
package ui.runner;
import javafx.fxml.FXML;
import ui.AppContext;
import ui.components.header.HeaderController;
import ui.runner.components.center.CenterController;


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
}


