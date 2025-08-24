package system.core.expand;

import system.core.model.Program;
import java.util.List;

public record ExpandedProgramResult(Program program, List<String> origins) {}
