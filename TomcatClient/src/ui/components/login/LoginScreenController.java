// java
package ui.components.login;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import ui.AppContext;
import ui.net.ApiClient;

public class LoginScreenController {
    @FXML private TextField usernameField;
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
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        errorLabel.setText("");
        if (username.isBlank()) {
            errorLabel.setText("Username is required");
            return;
        }
        setBusy(true);
        Task<ApiClient.LoginResult> task = new Task<>() {
            @Override protected ApiClient.LoginResult call() { return ctx.api().login(username); }
        };
        task.setOnSucceeded(e -> {
            setBusy(false);
            ApiClient.LoginResult r = task.getValue();
            if (r.success) {
                ctx.setUsername(username);
                onSuccess.run();
            } else {
                errorLabel.setText(r.error != null ? r.error : "Login failed");
            }
        });
        task.setOnFailed(e -> {
            setBusy(false);
            errorLabel.setText("Unexpected error");
        });
        new Thread(task, "login-thread").start();
    }

    private void setBusy(boolean busy) {
        if (loginButton != null) loginButton.setDisable(busy);
        if (usernameField != null) usernameField.setDisable(busy);
    }
}
