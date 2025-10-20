package server_core.util;

import system.core.exec.debugg.Debugger;

/** Server-held state for a single debug session (billing + debugger). */
public final class DebugSession {
    public final String id;
    public final String program;
    public final String arch;
    public final int degree;
    public final Debugger dbg;

    /** fixed arch cost charged at start */
    public final long fixed;
    /** cumulative variable charge (cycles) */
    public long chargedCycles;
    /** last snapshot's cycles, used to compute deltas per step/resume/back */
    public long lastCycles;

    public DebugSession(String id, String program, String arch, int degree,
                        Debugger dbg, long fixed, long chargedCycles, long lastCycles) {
        this.id = id;
        this.program = program;
        this.arch = arch;
        this.degree = degree;
        this.dbg = dbg;
        this.fixed = fixed;
        this.chargedCycles = chargedCycles;
        this.lastCycles = lastCycles;
    }
}
