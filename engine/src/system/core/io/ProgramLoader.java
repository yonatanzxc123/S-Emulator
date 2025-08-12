package system.core.io;

import system.core.model.Program;
import java.nio.file.Path;
import java.util.List;

public interface ProgramLoader {
    LoadOutcome load(Path xmlPath);

    record LoadOutcome(boolean ok, Program program, List<String> errors) {
        public static LoadOutcome ok(Program p){ return new LoadOutcome(true, p, List.of()); }
        public static LoadOutcome error(List<String> errs){ return new LoadOutcome(false, null, List.copyOf(errs)); }
    }
}
