package system.api;

import system.api.view.ProgramView;

import java.util.Map;

public record RunResult(long y, Map<String, Long> variables, long cycles, ProgramView executedProgram) {}
