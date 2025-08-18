package system.core.expand;

import system.core.expand.helpers.FreshNames;
import system.core.model.*;

public final class ExpanderImpl implements Expander {

    @Override
    public Program expandToDegree(Program program, int degree) {
        if (program == null || degree <= 0) return program;
        Program cur = program;
        for (int d = 0; d < degree; d++) {
            cur = expandOne(cur);
        }
        return cur;
    }

    private Program expandOne(Program p) {
        Program out = new Program(p.name() + " [expanded]");
        FreshNames fresh = new FreshNames(p);
        for (Instruction ins : p.instructions()) {
            if (ins.isBasic()) {
                out.add(ins);
            } else {
                ((SyntheticInstruction) ins).expandTo(out, fresh);
            }
        }
        return out;
    }
}
