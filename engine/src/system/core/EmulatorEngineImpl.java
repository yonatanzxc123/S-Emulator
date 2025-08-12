package system.core;

import system.api.*;
import system.core.io.ProgramLoaderJaxb;
import system.core.io.ProgramMapper;
import system.api.view.ProgramView;

import system.core.model.Program;


import java.nio.file.Files;
import java.nio.file.Path;

public final class EmulatorEngineImpl implements EmulatorEngine {
    private int version = 0;
    private Program current = null;
    private final java.util.List<HistoryEntry> history = new java.util.ArrayList<>();

    @Override
    public system.api.EmulatorEngine.LoadOutcome loadProgram(java.nio.file.Path xmlPath) {
        var outcome = new system.core.io.ProgramLoaderJaxb().load(xmlPath);
        if (!outcome.ok()) {
            return new system.api.EmulatorEngine.LoadOutcome(false, outcome.errors());
        }
        this.current = outcome.program();
        this.version++;
        this.history.clear(); // optional: reset history on new load
        return new system.api.EmulatorEngine.LoadOutcome(true, java.util.List.of());
    }





    @Override
    public RunResult run(int degree, java.util.List<Long> inputs) {
        if (current == null) return null;

        var exec = new system.core.exec.Executor();
        var st = exec.run(current, inputs);

        // build variables map for the UI
        java.util.Map<String, Long> vars = new java.util.LinkedHashMap<>();
        vars.put("y", st.y());
        var xs = new java.util.TreeMap<>(st.snapshotX());
        var zs = new java.util.TreeMap<>(st.snapshotZ());
        xs.forEach((i,v) -> vars.put("x"+i, v));
        zs.forEach((i,v) -> vars.put("z"+i, v));

        // save to history
        history.add(new HistoryEntry(history.size()+1, degree,
                inputs == null ? java.util.List.of() : java.util.List.copyOf(inputs),
                st.y(), st.cycles()));

        return new RunResult(st.y(), vars, st.cycles(), ProgramMapper.toView(current));
    }


    @Override
    public java.util.List<HistoryEntry> getRunHistory() {
        return java.util.List.copyOf(history);
    }





    @Override
    public int getVersion() { return version; }

    @Override
    public ProgramView getProgramView() {
        return (current == null) ? null : ProgramMapper.toView(current);
    }






}
