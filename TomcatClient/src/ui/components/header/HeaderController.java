// java
package ui.components.header;

import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class HeaderController {
    @FXML private Label userNameLbl;
    @FXML private Label titleLbl;

    public void bindUsername(StringProperty username) {
        if (userNameLbl != null) {
            userNameLbl.textProperty().bind(username);
        }
    }
    public void setTitleSuffix(String suffix) {
        if (titleLbl != null) {
            titleLbl.setText("S-Emulator - " + (suffix == null ? "" : suffix));
        }
    }
}
