package system.core;

import system.core.model.Program;
import system.api.HistoryEntry;
import java.io.Serializable;
import java.util.List;

record EngineSnapshot(int version, Program current, List<HistoryEntry> history)
        implements Serializable {}
