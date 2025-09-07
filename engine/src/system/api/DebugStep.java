package system.api;

import java.util.Map;

public record DebugStep(
        int stepNo,
        int pc,                 // PC after the step
        boolean finished,
        long cycles,
        Map<String,Long> vars,  // full snapshot (y, x1.., z1..)
        Map<String,Long> changed // only variables that changed this step
) {}