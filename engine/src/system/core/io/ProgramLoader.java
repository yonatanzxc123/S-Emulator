package system.core.io;

import system.core.model.Program;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface ProgramLoader {
    LoadOutcome load(Path xmlPath);

    record LoadOutcome(boolean ok, Program program,Map<String,Program> functions, List<String> errors)
    {
        public static LoadOutcome ok(Program p, Map<String,Program> func)
        {
            return new LoadOutcome(true, p, (func == null) ? Map.of() : Map.copyOf(func), List.of());
        }

        public static LoadOutcome error(List<String> errs)
        {
            return new LoadOutcome(false, null,Map.of(), List.copyOf(errs));
        }
    }
}
