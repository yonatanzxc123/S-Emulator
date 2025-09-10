package ui.components.inputtable;

import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import system.api.EmulatorEngine;
import system.api.view.ProgramView;
import ui.EngineInjector;

public class InputTableController implements EngineInjector {
    private EmulatorEngine engine;
    @Override
    public void setEngine(EmulatorEngine engine) {
        this.engine = engine;
    }

    @FXML
    private TableView<ProgramView> inputTable;
    @FXML
    private TableColumn<ProgramView,String> varColumn;
    @FXML
    private TableColumn<ProgramView,Integer> valueColumn;


}
