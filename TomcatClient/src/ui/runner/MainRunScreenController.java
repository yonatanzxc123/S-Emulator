
package ui.runner;
import javafx.fxml.FXML;
import ui.AppContext;
import ui.components.header.HeaderController;


// java
public class MainRunScreenController {
    private AppContext ctx;
    @FXML private HeaderController headerController;

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


