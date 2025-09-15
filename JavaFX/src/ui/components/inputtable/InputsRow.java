package ui.components.inputtable;

import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class InputsRow {
    private final StringProperty name = new SimpleStringProperty();
    private final LongProperty value = new SimpleLongProperty(0);

    public InputsRow(String name, long value) { this.name.set(name); this.value.set(value); }

    public String getName() { return name.get(); }
    public void setName(String n) { name.set(n); }
    public StringProperty nameProperty() { return name; }

    public long getValue() { return value.get(); }
    public void setValue(long v) { value.set(v); }
    public LongProperty valueProperty() { return value; }
}
