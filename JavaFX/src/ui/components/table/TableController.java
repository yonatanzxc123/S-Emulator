package ui.components.table;

import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import system.api.EmulatorEngine;
import ui.EngineInjector;

public class TableController implements EngineInjector {
    private EmulatorEngine engine;

    @Override public void setEngine(EmulatorEngine engine) { this.engine = engine; }

    @FXML private TableView table;
    @FXML private TableColumn lineColumn;
    @FXML private TableColumn bsColumn;
    @FXML private TableColumn labelColumn;
    @FXML private TableColumn instructionColumn;
    @FXML private TableColumn cyclesColumn;

    @FXML
    public void initialize() {
        lineColumn.setCellValueFactory(new PropertyValueFactory<>("line"));
        bsColumn.setCellValueFactory(new PropertyValueFactory<>("bs"));
        labelColumn.setCellValueFactory(new PropertyValueFactory<>("label"));
        instructionColumn.setCellValueFactory(new PropertyValueFactory<>("instruction"));
        cyclesColumn.setCellValueFactory(new PropertyValueFactory<>("cycles"));
    }


}
