// engine/src/system/api/EmulatorEngine.java
package system.api;

import system.api.view.ProgramView;
import system.core.exec.debugg.Debugger;

import java.nio.file.Path;
import java.util.List;

public interface EmulatorEngine {
    record LoadOutcome(boolean ok, List<String> errors) {}

    LoadOutcome loadProgram(Path xmlPath);
    LoadOutcome loadState(Path basePath);
    LoadOutcome saveState(Path basePath);
    int getVersion(); // for Sanity check for me

    ProgramView getProgramView();
    RunResult run(int degree, List<Long> inputs);
    List<HistoryEntry> getRunHistory();
    ProgramView getExpandedProgramView(int degree);
    int getMaxDegree();

    //Debugger
    Debugger startDebug(int degree, java.util.List<Long> inputs);

}
