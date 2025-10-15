// java
package ui.components.header;

import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class HeaderController {
    @FXML private Label userNameLbl; // matches `header.fxml`

    public void bindUsername(StringProperty username) {
        if (userNameLbl != null) {
            userNameLbl.textProperty().bind(username);
        }
    }
}
