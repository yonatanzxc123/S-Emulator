package ui.dashboard.components.program_table;

import javafx.beans.property.*;

public class ProgramRow {
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty uploaderName = new SimpleStringProperty();
    private final IntegerProperty instrCount = new SimpleIntegerProperty();
    private final IntegerProperty maxDegree = new SimpleIntegerProperty();
    private final IntegerProperty timesRun = new SimpleIntegerProperty();
    private final DoubleProperty creditsCost = new SimpleDoubleProperty();

    public ProgramRow(String name, String uploaderName, int instrCount, int maxDegree, int timesRun, double creditsCost) {
        this.name.set(name);
        this.uploaderName.set(uploaderName);
        this.instrCount.set(instrCount);
        this.maxDegree.set(maxDegree);
        this.timesRun.set(timesRun);
        this.creditsCost.set(creditsCost);
    }

    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }

    public String getUploaderName() { return uploaderName.get(); }
    public StringProperty uploaderNameProperty() { return uploaderName; }

    public int getInstrCount() { return instrCount.get(); }
    public IntegerProperty instrCountProperty() { return instrCount; }

    public int getMaxDegree() { return maxDegree.get(); }
    public IntegerProperty maxDegreeProperty() { return maxDegree; }

    public int getTimesRun() { return timesRun.get(); }
    public IntegerProperty timesRunProperty() { return timesRun; }

    public double getCreditsCost() { return creditsCost.get(); }
    public DoubleProperty creditsCostProperty() { return creditsCost; }
}
