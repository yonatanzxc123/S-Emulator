package server_core;

import system.core.EmulatorEngineImpl;
import system.core.model.Program;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public final class ProgramMeta {
    public final String name;
    public final String ownerUser;

    // Snapshot from load
    public final Set<String> providesFunctions; // keys from engine.getFunctions()
    public final Program mainProgram;           // for arch summary scans
    public final int instrCountDeg0;
    public final int maxDegree;

    // Runtime stats
    public final AtomicLong runsCount = new AtomicLong(0);
    public volatile double avgCreditsCost = 0;

    // Engine instance ready to run this program
    public final EmulatorEngineImpl engine;

    public ProgramMeta(String name, String ownerUser,
                       Set<String> providesFunctions, Program mainProgram,
                       int instrCountDeg0, int maxDegree,
                       EmulatorEngineImpl engine) {
        this.name = name;
        this.ownerUser = ownerUser;
        this.providesFunctions = providesFunctions;
        this.mainProgram = mainProgram;
        this.instrCountDeg0 = instrCountDeg0;
        this.maxDegree = maxDegree;
        this.engine = engine;
    }
}
