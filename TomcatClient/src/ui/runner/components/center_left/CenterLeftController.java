// java
package ui.runner.components.center_left;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import ui.net.ApiClient;
import ui.runner.components.instruction_table.InstructionTableController;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CenterLeftController {
    @FXML private Button expandBtn;
    @FXML private Label currDegreeLbl;
    @FXML private ChoiceBox<String> chooseVarBox;

    // Must match fx:id + "Controller" for <fx:include fx:id="instructionTable"/> and "ancestryTable"
    @FXML private InstructionTableController instructionTableController;
    @FXML private InstructionTableController ancestryTableController;

    private String programName;
    private int maxDegree = 0;
    private int currDegree = 0;

    private List<String> allVarsAndLabels = new ArrayList<>();

    @FXML
    private void initialize() {

        updateDegreeLabel();
        instructionTableController.getTable().getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                showAncestry(selected);
            } else {
                ancestryTableController.setInstructions(List.of());
            }
        });
    }

    // Called by CenterController after injection
    public void init(String program, int maxDegree) {
        this.programName = program;
        this.maxDegree = Math.max(0, maxDegree);
        if (currDegree > this.maxDegree) currDegree = this.maxDegree;
        updateDegreeLabel();

        // Ensure initial load reflects the current program + degree
        if (instructionTableController != null && programName != null && !programName.isBlank()) {
            instructionTableController.loadInstructions(programName, currDegree);
        }

        ObservableList<ApiClient.ProgramInstruction> items = instructionTableController.getTable().getItems();
        allVarsAndLabels.clear();
        var vars = new java.util.TreeSet<String>();
        var labels = new java.util.TreeSet<String>();
        for (ApiClient.ProgramInstruction pi : items) {
            // Find xN/zN in op string
            String op = pi.getOp();
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\b([xz]\\d+)\\b").matcher(op);
            while (m.find()) vars.add(m.group(1));
            // Find label
            String lbl = pi.getLabel();
            if (lbl != null && !lbl.isBlank()) labels.add(lbl);
        }
        allVarsAndLabels.addAll(vars);
        allVarsAndLabels.addAll(labels);
        chooseVarBox.getItems().setAll(allVarsAndLabels);
        chooseVarBox.setValue(null);

        // Add listener for highlighting
        chooseVarBox.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            highlightRows(sel);
        });
    }

    private void showAncestry(ApiClient.ProgramInstruction selected) {
        String chain = selected.getOriginChain();
        List<ApiClient.ProgramInstruction> ancestry = buildAncestryChain(chain);
        System.out.println(selected.getOriginChain());
        ancestryTableController.setInstructions(ancestry);
    }

    private List<ApiClient.ProgramInstruction> buildAncestryChain(String chain) {
        if (chain == null || chain.isBlank()) return List.of();

        String[] parts = chain.split("\\s*>>>\\s*");
        List<String> links = new ArrayList<>();
        for (String p : parts) {
            String s = p.trim();
            if (!s.isEmpty()) links.add(s);
        }
        if (links.isEmpty()) return List.of(); // Only original command, no ancestry

        java.util.Collections.reverse(links);

        List<ApiClient.ProgramInstruction> ancestry = new ArrayList<>();
        for (String link : links) {
            ancestry.add(parseAncestryStep(link));
        }
        return ancestry;
    }

    private ApiClient.ProgramInstruction parseAncestryStep(String link) {
        try {
            int idx = link.indexOf('#');
            int idxEnd = link.indexOf(' ', idx + 1);
            int index = Integer.parseInt(link.substring(idx + 1, idxEnd).trim());

            int bsStart = link.indexOf('(', idxEnd);
            int bsEnd = link.indexOf(')', bsStart);
            String bs = link.substring(bsStart + 1, bsEnd).trim();

            int labelStart = link.indexOf('[', bsEnd);
            int labelEnd = link.indexOf(']', labelStart);
            String label = "";
            int afterLabel = bsEnd + 1;
            if (labelStart != -1 && labelEnd > labelStart) {
                label = link.substring(labelStart + 1, labelEnd).trim();
                afterLabel = labelEnd + 1;
            }

            int cyclesStart = link.lastIndexOf('(');
            int cyclesEnd = link.lastIndexOf(')');
            int cycles = Integer.parseInt(link.substring(cyclesStart + 1, cyclesEnd).trim());

            String op = link.substring(afterLabel, cyclesStart).trim();

            return new ApiClient.ProgramInstruction(index, op, "", bs, label, cycles, "");
        } catch (Exception ex) {
            return new ApiClient.ProgramInstruction(0, link, "", "", "", 0, "");
        }
    }

    // Highlight rows in the instruction table containing the selected var/label
    private void highlightRows(String sel) {
        if (sel == null || sel.isBlank()) {
            instructionTableController.getTable().setRowFactory(tv -> new TableRow<>());
        } else {
            // Use regex to match exact variable/label
            final String regex = "\\b" + java.util.regex.Pattern.quote(sel) + "\\b";
            final java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
            instructionTableController.getTable().setRowFactory(tv -> new TableRow<>() {
                @Override
                protected void updateItem(ApiClient.ProgramInstruction item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setStyle("");
                    } else {
                        boolean match = false;
                        if (item.getOp() != null && pattern.matcher(item.getOp()).find()) match = true;
                        if (item.getLabel() != null && item.getLabel().equals(sel)) match = true;
                        setStyle(match ? "-fx-background-color: yellow;" : "");
                    }
                }
            });
        }
        instructionTableController.getTable().refresh();
    }

    private void loadInstructionsAsync(String programName, int degree) {
        new Thread(() -> {
            List<ApiClient.ProgramInstruction> result;
            try {
                result = ui.net.ApiClient.get().programBody(programName, degree);
            } catch (Exception e) {
                result = List.of();
            }
            final List<ApiClient.ProgramInstruction> finalResult = result;
            javafx.application.Platform.runLater(() -> updateInstructionsUI(finalResult));
        }, "load-instructions-bg").start();
    }

    private void updateInstructionsUI(List<ApiClient.ProgramInstruction> loaded) {
        instructionTableController.setInstructions(loaded);

        // Recompute vars and labels
        allVarsAndLabels.clear();
        var vars = new java.util.TreeSet<String>();
        var labels = new java.util.TreeSet<String>();
        for (ApiClient.ProgramInstruction pi : loaded) {
            String op = pi.getOp();
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\b([xz]\\d+)\\b").matcher(op);
            while (m.find()) vars.add(m.group(1));
            String lbl = pi.getLabel();
            if (lbl != null && !lbl.isBlank()) labels.add(lbl);
        }
        allVarsAndLabels.addAll(vars);
        allVarsAndLabels.addAll(labels);
        chooseVarBox.getItems().setAll(allVarsAndLabels);

        // Clear highlights
        chooseVarBox.setValue(null);
        highlightRows(null);
    }

    @FXML
    private void onExpand() {
        if (programName == null || programName.isBlank()) return;

        List<String> options = new ArrayList<>();
        for (int d = 0; d <= maxDegree; d++) {
            options.add(d + (d == currDegree ? " (current)" : ""));
        }
        ChoiceDialog<String> dialog = new ChoiceDialog<>(currDegree + " (current)", options);
        dialog.setTitle("Pick degree");
        dialog.setHeaderText(null);
        dialog.setContentText("Degree:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(sel -> {
            try {
                int d = Integer.parseInt(sel.replace(" (current)", "").trim());
                if (d < 0 || d > maxDegree || d == currDegree) return;

                currDegree = d;
                updateDegreeLabel();

                if (instructionTableController != null) {
                    loadInstructionsAsync(programName, currDegree);
                }
            } catch (NumberFormatException ignore) {}
        });
    }

    private void updateDegreeLabel() {
        if (currDegreeLbl != null) {
            currDegreeLbl.setText("Degree " + currDegree + " / " + maxDegree);
        }
    }
}
