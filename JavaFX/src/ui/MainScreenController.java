package ui;

import javafx.fxml.FXML;
import system.api.EmulatorEngine;
import ui.components.center.CenterController;
import ui.components.header.HeaderController;

public class MainScreenController implements EngineInjector {

    private EmulatorEngine engine;

    @FXML
    private HeaderController headerController; // comes from fx:id="header"
    @FXML private CenterController centerController; // comes from fx:id="center"


    @Override
    public void setEngine(EmulatorEngine engine) {
        this.engine = engine;
    }

    @FXML
    private void initialize() {
        centerController.bindToHeaderLoader(headerController.hasFileLoaded());
        headerController.hasFileLoaded().addListener((obs, was, loaded) -> {
            if (loaded) {
                centerController.showDegree(0);
            }
        });
    }
}
