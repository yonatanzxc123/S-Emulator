package server_core.util;

import system.core.exec.debugg.Debugger;

import java.util.List;

/** Server-held state for a single debug session (billing + debugger). */
public final class DebugSession {
    public final String id;
    public final String program;
    public final String arch;
    public final int degree;
    public final Debugger dbg;
    public final List<Long> inputs;
    public final String functionName; // <-- add this

    public final long fixed;
    public long chargedCycles;
    public long lastCycles;

    public DebugSession(String id, String program, String arch, int degree,
                        Debugger dbg, long fixed, long chargedCycles, long lastCycles,
                        List<Long> inputs, String functionName) { // <-- add param
        this.id = id;
        this.program = program;
        this.arch = arch;
        this.degree = degree;
        this.dbg = dbg;
        this.fixed = fixed;
        this.chargedCycles = chargedCycles;
        this.lastCycles = lastCycles;
        this.inputs = inputs;
        this.functionName = functionName; // <-- set field
    }
}
