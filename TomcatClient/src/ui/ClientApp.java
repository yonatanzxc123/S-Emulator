// java
package ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ui.components.login.LoginScreenController;
import ui.dashboard.MainDashboardScreenController;
import ui.net.ApiClient;
import ui.runner.MainRunScreenController;

public class ClientApp extends Application {
    private Stage stage;
    private AppContext ctx;
    private static ClientApp INSTANCE;

    public ClientApp() {
        INSTANCE = this;
    }

    public static ClientApp get() {
        return INSTANCE;
    }

    private MainRunScreenController runScreenController;

    public MainRunScreenController getRunScreenController() {
        return runScreenController;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.stage = primaryStage;

        String base = System.getProperty("api.base", "http://localhost:8080/server_Web_exploded");
        System.out.println("API base = " + base);
        this.ctx = new AppContext(new ApiClient(base, true));

        showLogin();
        stage.show();
    }

    @Override
    public void stop() {
        try {
            if (ctx != null && ctx.api() != null) {
                // Invalidate server session but keep the remembered username locally
                ctx.api().logout(true);
            }
        } catch (Exception ignored) {
        }
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

    public void showDashboard() throws Exception {
        FXMLLoader fxml = new FXMLLoader(ClientApp.class.getResource("/ui/dashboard/MainDashboardScreen.fxml"));
        fxml.setControllerFactory(type -> {
            if (type == MainDashboardScreenController.class) {
                MainDashboardScreenController ctrl = new MainDashboardScreenController(ctx);
                ctrl.setClientApp(this); // Pass ClientApp instance
                return ctrl;
            }
            try { return type.getDeclaredConstructor().newInstance(); }
            catch (Exception e) { throw new RuntimeException(e); }
        });
        stage.setScene(new Scene(fxml.load()));
        stage.sizeToScene();
        stage.setHeight(800);
        stage.setWidth(1000);
        Platform.runLater(stage::centerOnScreen);
        stage.setTitle("Dashboard");
    }
    public void showRunScreen() throws Exception {
        FXMLLoader fxml = new FXMLLoader(ClientApp.class.getResource("/ui/runner/MainRunScreen.fxml"));
        fxml.setControllerFactory(type -> {
            if (type == MainRunScreenController.class) {
                runScreenController = new MainRunScreenController(ctx);
                return runScreenController;
            }
            try { return type.getDeclaredConstructor().newInstance(); }
            catch (Exception e) { throw new RuntimeException(e); }
        });
        stage.setScene(new Scene(fxml.load()));
        stage.sizeToScene();
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        stage.setWidth(1200);
        stage.setHeight(800);
        stage.setResizable(true);
        Platform.runLater(stage::centerOnScreen);
        stage.setTitle("Run Program");
    }

}
