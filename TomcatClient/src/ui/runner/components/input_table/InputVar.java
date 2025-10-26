package ui.runner.components.input_table;

import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class InputVar {
    private final StringProperty name = new SimpleStringProperty();
    private final LongProperty value = new SimpleLongProperty(0);

    public InputVar(String name) { this.name.set(name); }
    public String getName() { return name.get(); }
    public void setName(String n) { name.set(n); }
    public StringProperty nameProperty() { return name; }

    public long getValue() { return value.get(); }
    public void setValue(long v) { value.set(v); }
    public LongProperty valueProperty() { return value; }
}
