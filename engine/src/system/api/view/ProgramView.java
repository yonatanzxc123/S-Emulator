package system.api.view;

import java.util.List;

public record ProgramView(
        String name,                      // program name
        List<String> inputsUsed,          // x1, x2, ...
        List<String> labelsInOrder,       // L1, L2, ..., EXIT last if present
        List<CommandView> commands        // per-instruction view
) {}
