package server_core;

import system.core.model.Program;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ProgramRegistry {

    private ProgramRegistry() {}

    private static final Map<String, ProgramMeta> PROGRAMS = new LinkedHashMap<>();
    private static final Map<String, Program> MAIN_PROGRAMS = new LinkedHashMap<>();
    private static final Map<String, String> PROGRAM_OWNER = new LinkedHashMap<>();
    private static final Map<String, ProgramsServlet.Stats> STATS = new LinkedHashMap<>();
    private static final Map<String, FunctionMeta> FUNCTIONS = new LinkedHashMap<>();

    public static synchronized void register(String name, ProgramMeta meta) {
        PROGRAMS.put(name, meta);
        MAIN_PROGRAMS.put(name, meta.mainProgram);
        PROGRAM_OWNER.put(name, meta.ownerUser);
        STATS.put(name, new ProgramsServlet.Stats(meta.instrCountDeg0, meta.maxDegree));

        for (String fn : meta.providesFunctions) {
            FunctionMeta fmeta = new FunctionMeta(fn, name, meta.ownerUser, meta.instrCountDeg0, meta.maxDegree);
            FUNCTIONS.put(fn, fmeta);
        }
    }

    public static synchronized ProgramMeta get(String programName) {
        return PROGRAMS.get(programName);
    }

    public static synchronized Program getMainProgram(String programName) {
        return MAIN_PROGRAMS.get(programName);
    }

    public static synchronized String getOwner(String programName) {
        return PROGRAM_OWNER.get(programName);
    }

    public static synchronized List<FunctionMeta> functionsOfProgram(String programName) {
        var out = new java.util.ArrayList<FunctionMeta>();
        for (FunctionMeta fm : FUNCTIONS.values()) {
            if (programName.equals(fm.definedInProgram())) out.add(fm);
        }
        return out;
    }

    public static synchronized Map<String, Program> copyMainPrograms() {
        return new LinkedHashMap<>(MAIN_PROGRAMS);
    }

    public static synchronized boolean containsProgram(String programName) {
        return MAIN_PROGRAMS.containsKey(programName);
    }
}
