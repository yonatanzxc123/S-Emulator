package ui.dashboard.components.program_table;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class ProgramTableController {
    @FXML private TableView<ProgramRow> table;
    @FXML private TableColumn<ProgramRow, String> nameCol;
    @FXML private TableColumn<ProgramRow, String> uploaderCol;
    @FXML private TableColumn<ProgramRow, Number> instrCol;
    @FXML private TableColumn<ProgramRow, Number> degreeCol;
    @FXML private TableColumn<ProgramRow, Number> runsCol;
    @FXML private TableColumn<ProgramRow, Number> costCol;

    private final ObservableList<ProgramRow> items = FXCollections.observableArrayList();

    public void init() {
        if (table != null) table.setItems(items);
        if (nameCol != null) nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        if (uploaderCol != null) uploaderCol.setCellValueFactory(new PropertyValueFactory<>("uploaderName"));
        if (instrCol != null) instrCol.setCellValueFactory(new PropertyValueFactory<>("instrCount"));
        if (degreeCol != null) degreeCol.setCellValueFactory(new PropertyValueFactory<>("maxDegree"));
        if (runsCol != null) runsCol.setCellValueFactory(new PropertyValueFactory<>("timesRun"));
        if (costCol != null) costCol.setCellValueFactory(new PropertyValueFactory<>("creditsCost"));
    }

    public void addProgram(String name, String uploader, int instrDeg0, int maxDegree) {
        items.add(new ProgramRow(name, uploader, instrDeg0, maxDegree, 0, 0));
    }
}
