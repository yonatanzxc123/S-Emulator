// java
package ui;

import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import ui.net.ApiClient;

public class AppContext {
    private final ApiClient api;
    private final StringProperty username = new SimpleStringProperty("");
    private final LongProperty credits = new SimpleLongProperty(0);


    public AppContext(ApiClient api) {
        this.api = api;
    }

    public ApiClient api() { return api; }
    public StringProperty usernameProperty() { return username; }
    public String getUsername() { return username.get(); }
    public void setUsername(String name) { username.set(name); }

    public LongProperty creditsProperty() { return credits; }
    public long getCredits() { return credits.get(); }
    public void setCredits(long value) { credits.set(value); }
}
