package system.core.expand;

import system.core.model.Instruction;
import system.core.model.Program;

import java.util.ArrayList;
import java.util.List;

public final class ExpansionService {
    private final List<SugarExpander> expanders;

    public ExpansionService(List<SugarExpander> expanders) {
        this.expanders = List.copyOf(expanders);
    }

    public Program expand(Program p, int degree) {
        if (degree <= 0) return p;
        Program cur = p;
        for (int d = 0; d < degree; d++) {
            cur = onePass(cur);
        }
        return cur;
    }

    private Program onePass(Program p) {
        var fresh = FreshNames.from(p);
        var out = new Program(p.name);
        for (Instruction ins : p.instructions) {
            boolean expanded = false;
            for (var ex : expanders) {
                if (ex.supports(ins)) {
                    out.instructions.addAll(ex.expand(ins, fresh));
                    expanded = true;
                    break;
                }
            }
            if (!expanded) out.instructions.add(ins);
        }
        return out;
    }

    public int maxDegree(Program p) {
        // naive for now: 0 , we'll take a closer look later
        return 0;
    }
}
