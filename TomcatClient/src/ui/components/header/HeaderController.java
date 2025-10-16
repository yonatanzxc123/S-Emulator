// java
package ui.components.header;

import javafx.beans.property.LongProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class HeaderController {
    @FXML private Label userNameLbl;
    @FXML private Label titleLbl;
    @FXML private Label creditsLbl;

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
    public void bindCredits(LongProperty credits) {
        if (creditsLbl != null) {
            creditsLbl.textProperty().bind(credits.asString("Credits: %d"));
        }
    }
}
