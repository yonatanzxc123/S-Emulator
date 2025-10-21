package ui.dashboard;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import ui.AppContext;
import ui.ClientApp;
import ui.components.header.HeaderController;
import ui.dashboard.components.center.CenterController;
import ui.net.ApiClient;

import java.util.Optional;

public class MainDashboardScreenController {
    private final AppContext ctx;
    private final StringProperty username = new SimpleStringProperty("");

    public MainDashboardScreenController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML private HeaderController headerController;
    @FXML private CenterController centerController;

    private ClientApp app;
    public void setClientApp(ClientApp app) { this.app = app; }

    @FXML
    private void initialize() {

        String u = null;
        try {
            u = (ctx != null && ctx.api() != null) ? ctx.api().currentUser() : ApiClient.get().currentUser();
        } catch (Exception ignore) { }
        if (u == null || u.isBlank()) u = "User";
        username.set(u);
        if (ctx != null) {
            ctx.setUsername(u);
        }

        if (headerController != null && ctx != null) {
            headerController.bindUsername(username);
            headerController.setTitleSuffix("Dashboard");
            headerController.bindCredits(ctx.creditsProperty());
        }
        if (centerController != null && ctx != null) {
            centerController.setAppContext(ctx);
            centerController.setClientApp(app);
            centerController.init(ctx);
        }
        fetchInitialCredits();
    }
    private void fetchInitialCredits() {
        Thread t = new Thread(() -> {
            try {
                Optional<ApiClient.UserOnline> me = ctx.api().usersOnline().stream()
                        .filter(u -> u.name.equals(ctx.getUsername()))
                        .findFirst();

                me.ifPresent(userOnline -> Platform.runLater(() -> ctx.setCredits(userOnline.credits)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        t.setDaemon(true);
        t.start();
    }
    public void updateCredits(long newTotal) {
        if (ctx != null) {
            ctx.setCredits(newTotal);
        }
    }
}
