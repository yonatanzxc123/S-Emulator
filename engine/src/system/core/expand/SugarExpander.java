package system.core.expand;

import system.core.model.Instruction;
import java.util.List;

public interface SugarExpander {
    boolean supports(Instruction i);
    List<Instruction> expand(Instruction i, FreshNames fresh); // may still contain synthetics just so I know
}
