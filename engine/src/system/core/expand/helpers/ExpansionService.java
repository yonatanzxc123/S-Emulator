package system.core.expand.helpers;

import system.core.expand.syntheticinterface.SugarExpander;
import system.core.model.Instruction;
import system.core.model.Program;
import system.core.expand.sugars.*;

import java.util.List;

public final class ExpansionService {

    /** Small holder for one expansion round result. */
    public static final class Round {
        public final Program program;
        public final boolean changed;
        public Round(Program program, boolean changed) {
            this.program = program;
            this.changed = changed;
        }
    }

    private final List<SugarExpander> expanders = List.of(
            new ZeroVariableExpander(),
            new GotoLabelExpander(),
            new ConstantAssignmentExpander(),
            new AssignmentExpander(),
            new JumpZeroExpander()
            // (later) add JumpEqualConstantExpander, JumpEqualVariableExpander, ...
    );

    /** Expand up to 'degree' rounds, but stop early if a round makes no changes. */
    public Program expandToDegree(Program program, int degree) {
        if (program == null || degree <= 0) return program;
        Program cur = program;
        for (int d = 0; d < degree; d++) {
            Round r = expandOne(cur);
            cur = r.program;
            if (!r.changed) break; // stop as soon as nothing changed
        }
        return cur;
    }

    /** Perform exactly one expansion round and report if anything changed. */
    public Round expandOne(Program p) {
        Program out = new Program(p.name + " [expanded]");
        FreshNames fresh = new FreshNames(p);
        boolean changed = false;

        for (Instruction ins : p.instructions) {
            boolean handled = false;
            for (var ex : expanders) {
                if (ex.canHandle(ins)) {
                    ex.expand(ins, out, fresh);
                    handled = true;
                    changed = true;
                    break;
                }
            }
            if (!handled) {
                out.instructions.add(ins); // pass-through
            }
        }
        return new Round(out, changed);
    }

    /** How many rounds can we actually expand with the expanders we have (stop when a round makes no changes). */
    public int computeMaxDegree(Program program, int cap) {
        if (program == null) return 0;
        int d = 0;
        Program cur = program;
        while (d < cap) {
            Round r = expandOne(cur);
            if (!r.changed) break;
            cur = r.program;
            d++;
        }
        return d;
    }
}
