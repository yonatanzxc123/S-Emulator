// java
package ui.components.header;

import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class HeaderController {
    @FXML private Label usernameLabel;

    public void bindUsername(StringProperty username) {
        if (usernameLabel != null) {
            usernameLabel.textProperty().bind(username);
        }
    }
}
