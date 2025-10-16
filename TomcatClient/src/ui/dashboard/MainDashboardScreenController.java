package ui.dashboard;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import ui.AppContext;
import ui.components.header.HeaderController;
import ui.dashboard.components.center.CenterController;
import ui.net.ApiClient;

public class MainDashboardScreenController {
    private final AppContext ctx;
    private final StringProperty username = new SimpleStringProperty("");

    public MainDashboardScreenController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML private HeaderController headerController;
    @FXML private CenterController centerController;

    @FXML
    private void initialize() {

        String u = null;
        try {
            u = (ctx != null && ctx.api() != null) ? ctx.api().currentUser() : ApiClient.get().currentUser();
        } catch (Exception ignore) { }
        if (u == null || u.isBlank()) u = "User";
        username.set(u);

        if (headerController != null && ctx != null) {
            headerController.bindUsername(username);
            headerController.setTitleSuffix("Dashboard");
        }
        if (centerController != null && ctx != null) {
            centerController.init(ctx);
        }
    }
}
