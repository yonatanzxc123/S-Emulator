package ui.dashboard.components.function_table;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class FunctionRow {
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty program = new SimpleStringProperty();

    public FunctionRow(String name, String program) {
        this.name.set(name);
        this.program.set(program);
    }

    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }

    public String getProgram() { return program.get(); }
    public StringProperty programProperty() { return program; }
}
