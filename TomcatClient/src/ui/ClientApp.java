// java
// File: 'TomcatClient/src/ui/ClientApp.java'
package ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ui.dashboard.MainDashboardScreenController;
import ui.net.ApiClient;
import ui.components.login.LoginScreenController;

public class ClientApp extends Application {
    private Stage stage;
    private AppContext ctx;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.stage = primaryStage;

        String base = System.getProperty("api.base", "http://localhost:8080/server");
        System.out.println("API base = " + base);
        this.ctx = new AppContext(new ApiClient(base, true)); // debug=true logs full URLs

        if (!ctx.api().health()) {
            System.err.println("Health check failed at: " + base + "/api/health");
        }

        showLogin();
        stage.show();
    }

    private void showLogin() throws Exception {
        FXMLLoader fxml = new FXMLLoader(ClientApp.class.getResource("/ui/components/login/login_screen.fxml"));
        fxml.setControllerFactory(t -> new LoginScreenController(ctx, () -> {
            try { showDashboard(); } catch (Exception e) { throw new RuntimeException(e); }
        }));
        stage.setScene(new Scene(fxml.load()));
        stage.setTitle("Login");
    }

    private void showDashboard() throws Exception {
        FXMLLoader fxml = new FXMLLoader(ClientApp.class.getResource("/ui/dashboard/MainDashboardScreen.fxml"));
        fxml.setControllerFactory(t -> new MainDashboardScreenController(ctx));
        stage.setScene(new Scene(fxml.load()));
        stage.setTitle("Dashboard");
    }
}
