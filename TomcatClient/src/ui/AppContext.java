// java
package ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import ui.net.ApiClient;

public class AppContext {
    private final ApiClient api;
    private final StringProperty username = new SimpleStringProperty("");

    public AppContext(ApiClient api) {
        this.api = api;
    }

    public ApiClient api() { return api; }
    public StringProperty usernameProperty() { return username; }
    public String getUsername() { return username.get(); }
    public void setUsername(String name) { username.set(name); }
}
