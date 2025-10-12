// java
package ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ui.components.login.LoginScreenController;
import ui.dashboard.MainDashboardScreenController;
import ui.net.ApiClient;

public class ClientApp extends Application {
    private Stage stage;
    private AppContext ctx;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.stage = primaryStage;

        String base = System.getProperty("api.base", "http://localhost:8080/server");
        System.out.println("API base = " + base);
        this.ctx = new AppContext(new ApiClient(base, true));

        if (!ctx.api().health()) {
            System.err.println("Health check failed at: " + base + "/api/health");
        }

        showLogin();
        stage.show();
    }

    private void showLogin() throws Exception {
        FXMLLoader fxml = new FXMLLoader(ClientApp.class.getResource("/ui/components/login/login_screen.fxml"));
        fxml.setControllerFactory(type -> {
            if (type == LoginScreenController.class) {
                return new LoginScreenController(ctx, () -> {
                    try { showDashboard(); } catch (Exception e) { throw new RuntimeException(e); }
                });
            }
            try { return type.getDeclaredConstructor().newInstance(); }
            catch (Exception e) { throw new RuntimeException(e); }
        });
        stage.setScene(new Scene(fxml.load()));
        stage.setTitle("Login");
    }

    private void showDashboard() throws Exception {
        FXMLLoader fxml = new FXMLLoader(ClientApp.class.getResource("/ui/dashboard/MainDashboardScreen.fxml"));
        fxml.setControllerFactory(type -> {
            if (type == MainDashboardScreenController.class) {
                return new MainDashboardScreenController(ctx);
            }
            try { return type.getDeclaredConstructor().newInstance(); }
            catch (Exception e) { throw new RuntimeException(e); }
        });
        stage.setScene(new Scene(fxml.load()));
        stage.setTitle("Dashboard");
    }
}
