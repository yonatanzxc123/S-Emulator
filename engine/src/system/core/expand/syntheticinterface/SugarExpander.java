package system.core.expand.syntheticinterface;

import system.core.model.Instruction;
import system.core.model.Program;
import system.core.expand.helpers.FreshNames;

public interface SugarExpander {
    boolean canHandle(Instruction ins);
    void expand(Instruction ins, Program out, FreshNames fresh);
}
