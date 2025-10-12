package ui.dashboard.components.program_table;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ProgramRow {
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty owner = new SimpleStringProperty();

    public ProgramRow(String name, String owner) {
        this.name.set(name);
        this.owner.set(owner);
    }

    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }

    public String getOwner() { return owner.get(); }
    public StringProperty ownerProperty() { return owner; }
}
