package ui.components.center;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableBooleanValue;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import system.api.EmulatorEngine;
import ui.EngineInjector;


public class CenterController implements EngineInjector {
    private EmulatorEngine engine;

    @Override public void setEngine(EmulatorEngine engine) { this.engine = engine; }

    @FXML
    private Button programSelectorBtn;
    @FXML
    private Button collapseBtn;
    @FXML
    private Label currDegreeLbl;
    @FXML
    private Button expandBtn;
    @FXML
    private ChoiceBox<String> chooseVarBtn;


    @FXML
    private void initialize() {
        currDegreeLbl.prefHeightProperty().bind(programSelectorBtn.heightProperty());
        chooseVarBtn.prefHeightProperty().bind(programSelectorBtn.heightProperty());
        programSelectorBtn.setDisable(true);
        collapseBtn.setDisable(true);
        expandBtn.setDisable(true);
    }

    public void bindToHeaderLoader(ObservableBooleanValue loaded){
        programSelectorBtn.disableProperty().bind(Bindings.not(loaded));
        collapseBtn.disableProperty().bind(Bindings.not(loaded));
        expandBtn.disableProperty().bind(Bindings.not(loaded));
        chooseVarBtn.disableProperty().bind(Bindings.not(loaded));
    }



}
