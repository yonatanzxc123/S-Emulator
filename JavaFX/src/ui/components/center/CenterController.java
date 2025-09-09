package ui.components.center;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import system.api.EmulatorEngine;
import ui.EngineInjector;
import ui.components.table.TableController;


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

    @FXML private ui.components.table.TableController instructionTableController;

    private SimpleIntegerProperty currDegree = new SimpleIntegerProperty(0);
    private SimpleIntegerProperty maxDegree = new SimpleIntegerProperty(0);


    @FXML
    private void initialize() {
        currDegreeLbl.prefHeightProperty().bind(programSelectorBtn.heightProperty());
        chooseVarBtn.prefHeightProperty().bind(programSelectorBtn.heightProperty());
        programSelectorBtn.setDisable(true);
        collapseBtn.setDisable(true);
        expandBtn.setDisable(true);
        currDegreeLbl.setText("Current / Maximum degree");

    }

    public void bindToHeaderLoader(ObservableBooleanValue loaded){
        programSelectorBtn.disableProperty().bind(Bindings.not(loaded));
        collapseBtn.disableProperty().bind(
                Bindings.or(Bindings.not(loaded), currDegree.isEqualTo(0))
        );
        expandBtn.disableProperty().bind(
                Bindings.or(Bindings.not(loaded), currDegree.greaterThanOrEqualTo(maxDegree))
        );
        chooseVarBtn.disableProperty().bind(Bindings.not(loaded));

        StringBinding labelText = Bindings.createStringBinding(
                () -> loaded.get()
                        ? (currDegree.get() + " / " + maxDegree.get())
                        : "Current / Maximum degree",
                loaded, currDegree, maxDegree
        );
        currDegreeLbl.textProperty().bind(labelText);

        loaded.addListener((obs, was, isNowLoaded) -> {
            if (isNowLoaded) {
                int md = (engine == null) ? 0 : engine.getMaxDegree();
                maxDegree.set(md);
                currDegree.set(Math.min(currDegree.get(), md));
                if (instructionTableController != null) {
                    instructionTableController.showDegree(currDegree.get());
                }
            } else {
                maxDegree.set(0);
                currDegree.set(0);
            }
        });

    }
    public void showDegree(int degree) {
        if (instructionTableController != null) {
            instructionTableController.showDegree(degree);
            currDegree.set(degree);
        }
    }

    public void onActionExpansion(){
        if(engine.getMaxDegree() >= currDegree.get()) {
            currDegree.set(currDegree.get() + 1);
            instructionTableController.showDegree(currDegree.get());
        }
    }

    public void onActionCollapse(){
        if(currDegree.get() > 0 ) {
            currDegree.set(currDegree.get() - 1);
            instructionTableController.showDegree(currDegree.get());
        }

    }


}
