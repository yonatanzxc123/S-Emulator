// java
package ui.components.login;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import ui.AppContext;
import ui.net.ApiClient;

public class LoginScreenController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;

    private final AppContext ctx;
    private final Runnable onSuccess;

    public LoginScreenController(AppContext ctx, Runnable onSuccess) {
        this.ctx = ctx;
        this.onSuccess = onSuccess;
    }

    @FXML
    private void onLogin() {
        String username = (usernameField == null || usernameField.getText() == null)
                ? "" : usernameField.getText().trim();
        String password = (passwordField == null || passwordField.getText() == null)
                ? "" : passwordField.getText();

        if (errorLabel != null) errorLabel.setText("");
        if (username.isBlank()) {
            if (errorLabel != null) errorLabel.setText("Enter username");
            return;
        }

        setBusy(true);

        Task<ApiClient.LoginResult> task = new Task<>() {
            @Override
            protected ApiClient.LoginResult call() throws Exception {
                return ctx.api().login(username, password);
            }
        };

        task.setOnSucceeded(ev -> {
            setBusy(false);
            ApiClient.LoginResult res = task.getValue();
            if (res != null && res.success) {
                if (onSuccess != null) onSuccess.run();
            } else {
                if (errorLabel != null) {
                    errorLabel.setText((res == null || res.error == null || res.error.isBlank())
                            ? "Login failed" : res.error);
                }
            }
        });

        task.setOnFailed(ev -> {
            setBusy(false);
            if (errorLabel != null) {
                Throwable ex = task.getException();
                errorLabel.setText(ex == null ? "Login failed" : ex.getMessage());
            }
        });

        Thread t = new Thread(task, "login");
        t.setDaemon(true);
        t.start();
    }

    private void setBusy(boolean busy) {
        if (loginButton != null) loginButton.setDisable(busy);
        if (usernameField != null) usernameField.setDisable(busy);
        if (passwordField != null) passwordField.setDisable(busy);
    }
}
