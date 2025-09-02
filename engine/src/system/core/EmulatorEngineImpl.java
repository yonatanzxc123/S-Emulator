package system.core;

import system.api.EmulatorEngine;
import system.api.HistoryEntry;
import system.api.RunResult;
import system.api.view.ProgramView;
import system.core.exec.Executor;
import system.core.expand.Expander;
import system.core.expand.ExpanderImpl;
import system.core.io.ProgramLoaderJaxb;
import system.core.io.ProgramMapper;
import system.core.model.Instruction;
import system.core.model.Program;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.Map;

public final class EmulatorEngineImpl implements EmulatorEngine {
    private int version = 0;
    private Program current = null;
    private final List<HistoryEntry> history = new ArrayList<>();
    private Map<String,Program> functions = Map.of();
    private final Expander expander = new ExpanderImpl();

    @Override
    public LoadOutcome loadProgram(Path xmlPath) {
        var outcome = new ProgramLoaderJaxb().load(xmlPath);
        if (!outcome.ok()) {
            return new LoadOutcome(false, outcome.errors());
        }
        this.current = outcome.program();
        this.functions = outcome.functions();
        this.version++;
        this.history.clear(); // reset on new load
        return new LoadOutcome(true, List.of());
    }

    @Override
    public int getVersion() { return version; }

    public Map<String,Program> getFunctions() { return Map.copyOf(functions); }

    @Override
    public ProgramView getProgramView() {
        return (current == null) ? null : ProgramMapper.toView(current);
    }

    @Override
    public RunResult run(int degree, List<Long> inputs) {
        if (current == null) return null;

        int max = getMaxDegree();
        int use = Math.max(0, Math.min(degree, max));

        Program toRun;
        ProgramView executedView;

        if (use == 0) {
            // no expansion – original program, no origins
            toRun = current;
            executedView = ProgramMapper.toView(current);
        } else {
            // expand and keep origins for printing
            var exp = new system.core.expand.ExpanderImpl();
            var res = exp.expandToDegreeWithOrigins(current, use);
            toRun = res.program();
            executedView = ProgramMapper.toView(res.program(), res.origins());
        }

        var exec = new system.core.exec.Executor();
        var st = exec.run(toRun, inputs);

        var vars = new LinkedHashMap<String, Long>();
        vars.put("y", st.y());
        var xs = new TreeMap<>(st.snapshotX());
        var zs = new TreeMap<>(st.snapshotZ());
        xs.forEach((i, v) -> vars.put("x" + i, v));
        zs.forEach((i, v) -> vars.put("z" + i, v));

        history.add(new HistoryEntry(
                history.size() + 1,
                use,
                (inputs == null) ? List.of() : List.copyOf(inputs),
                st.y(),
                st.cycles()
        ));

        return new RunResult(
                st.y(),
                st.cycles(),
                executedView,   // <- now includes origins when degree>0
                vars
        );
    }

    @Override
    public List<HistoryEntry> getRunHistory() {
        return List.copyOf(history);
    }

    @Override
    public int getMaxDegree() {
        if (current == null) return 0;
        final int CAP = 1000; // its a  safety cap
        int d = 0;
        Program cur = current;
        while (d < CAP && containsSynthetic(cur)) {
            cur = expander.expandToDegree(cur, 1);
            d++;
        }
        return d;
    }

    @Override
    public ProgramView getExpandedProgramView(int degree) {
        if (current == null) return null;
        int max = getMaxDegree();
        int use = Math.max(0, Math.min(degree, max));
        if (use == 0) return ProgramMapper.toView(current);

        var exp = new system.core.expand.ExpanderImpl();
        var res = exp.expandToDegreeWithOrigins(current, use);
        return ProgramMapper.toView(res.program(), res.origins());
    }




    // --- Load/Save state---
    @Override
    public LoadOutcome saveState(Path basePath) {
        try {
            Path file = addExt(basePath, ".state");
            var snap = new EngineSnapshot(version, current, List.copyOf(history));
            try (var out = new ObjectOutputStream(Files.newOutputStream(file))) {
                out.writeObject(snap);
            }
            return new LoadOutcome(true, List.of());
        } catch (Exception e) {
            return new LoadOutcome(false, List.of("Save failed: " + e.getMessage()));
        }
    }

    @Override
    public LoadOutcome loadState(Path basePath) {
        try {
            Path file = addExt(basePath, ".state");
            try (var in = new ObjectInputStream(Files.newInputStream(file))) {
                var snap = (EngineSnapshot) in.readObject();
                this.version = snap.version();
                this.current = snap.current();
                this.history.clear();
                this.history.addAll(snap.history());
            }
            return new LoadOutcome(true, List.of());
        } catch (Exception e) {
            return new LoadOutcome(false, List.of("Load failed: " + e.getMessage()));
        }
    }

    private static Path addExt(Path p, String ext) {
        String s = p.toString();
        return s.endsWith(ext) ? p : Paths.get(s + ext);
    }



    // ---- helper ----
    private static boolean containsSynthetic(Program p) {
        for (Instruction ins : p.instructions()) {
            if (!ins.isBasic()) return true;
        }
        return false;
    }




}
