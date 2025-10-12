package ui.dashboard.components.function_table;

import javafx.beans.property.*;

public class FunctionRow {
    private final StringProperty programName = new SimpleStringProperty();
    private final StringProperty functionName = new SimpleStringProperty();
    private final StringProperty userName = new SimpleStringProperty();
    private final IntegerProperty instrCount = new SimpleIntegerProperty();
    private final IntegerProperty maxDegree = new SimpleIntegerProperty();

    public FunctionRow(String programName, String functionName, String userName, int instrCount, int maxDegree) {
        this.programName.set(programName);
        this.functionName.set(functionName);
        this.userName.set(userName);
        this.instrCount.set(instrCount);
        this.maxDegree.set(maxDegree);
    }

    public String getProgramName() { return programName.get(); }
    public StringProperty programNameProperty() { return programName; }

    public String getFunctionName() { return functionName.get(); }
    public StringProperty functionNameProperty() { return functionName; }

    public String getUserName() { return userName.get(); }
    public StringProperty userNameProperty() { return userName; }

    public int getInstrCount() { return instrCount.get(); }
    public IntegerProperty instrCountProperty() { return instrCount; }

    public int getMaxDegree() { return maxDegree.get(); }
    public IntegerProperty maxDegreeProperty() { return maxDegree; }
}
