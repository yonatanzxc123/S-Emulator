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
import system.api.view.CommandView;
import system.api.view.ProgramView;
import ui.EngineInjector;
import ui.components.table.TableController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class CenterController implements EngineInjector {
    private EmulatorEngine engine;

    @Override
    public void setEngine(EmulatorEngine engine) {
        this.engine = engine;
    }

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
    private TableController instructionTableController;
    @FXML
    private TableController historyTableController;

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

        instructionTableController.selectedCommandProperty().addListener((obs, oldSel, sel) -> {
            if (sel == null) {
                historyTableController.clear();
                return;
            }
            int line = sel.number();
            lineSelected(line, sel);
        });

    }

    public void bindToHeaderLoader(ObservableBooleanValue loaded) {
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

    public void onActionExpansion() {
        if (engine.getMaxDegree() >= currDegree.get()) {
            currDegree.set(currDegree.get() + 1);
            instructionTableController.showDegree(currDegree.get());
        }
    }

    public void onActionCollapse() {
        if (currDegree.get() > 0) {
            currDegree.set(currDegree.get() - 1);
            instructionTableController.showDegree(currDegree.get());
        }

    }

    public void lineSelected(int line, CommandView selected) {
        List<CommandView> rows = buildAncestryRows(selected);
        rows = renumber(rows);
        historyTableController.showProgramView(
                new ProgramView("Ancestry", List.of(), List.of(), rows)
        );
    }

    private static List<CommandView> renumber(List<CommandView> rows) {
        List<CommandView> out = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            CommandView c = rows.get(i);
            out.add(new CommandView(
                    i + 1,                // <- 1, 2, 3, ...
                    c.basic(),
                    c.labelOrEmpty(),
                    c.text(),
                    c.cycles(),
                    c.originChain()
            ));
        }
        return out;
    }

    private List<CommandView> buildAncestryRows(CommandView selected) {
        List<CommandView> out = new ArrayList<>();
        out.add(selected);

        String chain = selected.originChain();
        if (chain == null || chain.isBlank()) return out;

        String[] parts = chain.split("  >>>  ");
        List<String> links = new ArrayList<>();
        for (String p : parts) {
            String s = p.trim();
            if (!s.isEmpty()) links.add(s);
        }
        Collections.reverse(links);

        int rowNum = 1;
        for (String link : links) {
            out.add(parseLinkToCommandView(link, rowNum++));
        }
        return out;
    }

    private CommandView parseLinkToCommandView(String link, int fallbackNumber) {
        try {
            String s = link.trim();
            int pHash = s.indexOf('#');
            int pSp1 = s.indexOf(' ', pHash + 1);
            int number = Integer.parseInt(s.substring(pHash + 1, pSp1).trim());

            int pParL = s.indexOf('(', pSp1);
            int pParR = s.indexOf(')', pParL + 1);
            boolean basic = s.substring(pParL + 1, pParR).trim().equalsIgnoreCase("B");

            int pLbL = s.indexOf('[', pParR);
            int pLbR = (pLbL >= 0) ? s.indexOf(']', pLbL + 1) : -1;
            String label = "";
            int afterLabel = pParR + 1;
            if (pLbL >= 0 && pLbR > pLbL) {
                label = s.substring(pLbL + 1, pLbR).trim();
                afterLabel = pLbR + 1;
            }

            int pLastL = s.lastIndexOf('(');
            int pLastR = s.lastIndexOf(')');
            int cycles = Integer.parseInt(s.substring(pLastL + 1, pLastR).trim());

            String text = s.substring(afterLabel).trim();
            if (pLastL > afterLabel) {
                text = s.substring(afterLabel, pLastL).trim();
            }

            return new CommandView(number, basic, label, text, cycles, /*originChain*/ "");
        } catch (Exception ex) {
            return new CommandView(fallbackNumber, true, "", link, 0, "");
        }
    }
}



