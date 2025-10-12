package system.core;

import system.api.EmulatorEngine;
import system.api.HistoryEntry;
import system.api.RunResult;
import system.api.view.ProgramView;
import system.core.exec.*;
import system.core.exec.debugg.Debugger;
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
        if (current == null) return null;
        return FunctionEnv.with(new FunctionEnv(functions), () -> ProgramMapper.toView(current));
    }

    @Override
    public RunResult run(int degree, List<Long> inputs) {
        if (current == null) return null;

        return FunctionEnv.with(new FunctionEnv(functions), () -> {
            int max = getMaxDegree();
            int use = Math.max(0, Math.min(degree, max));

            Program toRun;
            ProgramView executedView;

            if (use == 0) {
                toRun = current;
                executedView = ProgramMapper.toView(current);
            } else {
                var res = new ExpanderImpl().expandToDegreeWithOrigins(current, use);
                toRun = res.program();
                executedView = ProgramMapper.toView(res.program(), res.origins());
            }

            var exec = new Executor();
            var st = exec.run(toRun, inputs);

            var vars = new LinkedHashMap<String, Long>();
            vars.put("y", st.y());
            var xs = new TreeMap<>(st.snapshotX());
            var zs = new TreeMap<>(st.snapshotZ());
            xs.forEach((i, v) -> vars.put("x" + i, v));
            zs.forEach((i, v) -> vars.put("z" + i, v));

            history.add(new HistoryEntry(
                    history.size() + 1, use,
                    (inputs == null) ? List.of() : List.copyOf(inputs),
                    st.y(), st.cycles(),vars
            ));

            return new RunResult(st.y(), st.cycles(), executedView, vars);
        });
    }

    @Override
    public List<HistoryEntry> getRunHistory() {
        return List.copyOf(history);
    }

    @Override
    public int getMaxDegree() {
        if (current == null) return 0;
        return FunctionEnv.with(new FunctionEnv(functions), () -> {
            final int CAP = 1000; // safety cap
            int d = 0;
            Program cur = current;
            while (d < CAP && containsSynthetic(cur)) {
                cur = expander.expandToDegree(cur, 1); // may expand QUOTE
                d++;
            }
            return d;
        });
    }

    @Override
    public ProgramView getExpandedProgramView(int degree) {
        if (current == null) return null;

        return FunctionEnv.with(new FunctionEnv(functions), () -> {
            int max = getMaxDegree();
            int use = Math.max(0, Math.min(degree, max));
            if (use == 0) return ProgramMapper.toView(current);

            var res = new ExpanderImpl().expandToDegreeWithOrigins(current, use);
            return ProgramMapper.toView(res.program(), res.origins());
        });
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

    public LoadOutcome loadProgramFromString(String xml) {
        var outcome = new ProgramLoaderJaxb().loadFromString(xml);
        if (!outcome.ok()) {
            return new LoadOutcome(false, outcome.errors());
        }
        this.current = outcome.program();
        this.functions = outcome.functions();
        this.version++;
        this.history.clear();
        return new LoadOutcome(true, List.of());
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

    // ---- Debugger ----
    @Override
    public Debugger startDebug(int degree, List<Long> inputs) {
        if (current == null) return null;
        return FunctionEnv.with(new FunctionEnv(functions), () -> {
            int max = getMaxDegree();             // now safe – already inside FunctionEnv
            int use = Math.max(0, Math.min(degree, max));

            final Program programToDebug = (use == 0)
                    ? current
                    : new ExpanderImpl().expandToDegree(current, use); // may expand QUOTE

            Debugger dbg = new Debugger();
            dbg.init(programToDebug, inputs, functions);
            return dbg;
        });
    }



}
