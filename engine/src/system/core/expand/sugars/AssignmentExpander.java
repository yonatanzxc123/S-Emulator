package system.core.expand.sugars;

import system.core.expand.syntheticinterface.SugarExpander;
import system.core.expand.helpers.FreshNames;

import system.core.model.Program;
import system.core.model.Instruction;
import system.core.model.Var;
import system.core.model.basic.*;
import system.core.model.synthetic.*;

public final class AssignmentExpander implements SugarExpander {
    @Override public boolean canHandle(Instruction ins) { return ins instanceof Assignment; }

    @Override
    public void expand(Instruction ins, Program out, FreshNames fresh) {
        Assignment asg = (Assignment) ins;
        Var dst = asg.v();
        Var src = asg.src();
        Var tmp = fresh.tempZ();

        // tmp <- 0
        new ZeroVariableExpander().expand(new ZeroVariable(asg.label(), tmp), out, fresh);
        // dst <- 0
        new ZeroVariableExpander().expand(new ZeroVariable("", dst), out, fresh);

        String CONSUME = fresh.nextLabel();
        String RESTORE = fresh.nextLabel();

        // if src!=0 goto CONSUME, else jump to RESTORE section
        out.instructions.add(new IfGoto("", src, CONSUME, 2));
        // fall-through into RESTORE gating
        out.instructions.add(new IfGoto("", tmp, RESTORE, 2)); // if tmp!=0 goto RESTORE
        // ---- CONSUME ----
        out.instructions.add(new Dec(CONSUME, src, 1));       // src--
        out.instructions.add(new Inc("", tmp, 1));            // tmp++
        out.instructions.add(new IfGoto("", src, CONSUME, 2));// loop until src==0
        // ---- RESTORE: while tmp!=0 { tmp--; src++; dst++; }
        out.instructions.add(new Dec(RESTORE, tmp, 1));
        out.instructions.add(new Inc("", src, 1));
        out.instructions.add(new Inc("", dst, 1));
        out.instructions.add(new IfGoto("", tmp, RESTORE, 2));
    }
}
