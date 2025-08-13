package system.core.expand.sugars;

import system.core.expand.syntheticinterface.SugarExpander;
import system.core.expand.helpers.FreshNames;

import system.core.model.Program;
import system.core.model.Instruction;
import system.core.model.Var;
import system.core.model.basic.*;
import system.core.model.synthetic.*;


public final class JumpZeroExpander implements SugarExpander {
    @Override public boolean canHandle(Instruction ins) { return ins instanceof JumpZero; }

    @Override
    public void expand(Instruction ins, Program out, FreshNames fresh) {
        JumpZero jz = (JumpZero) ins;
        String SKIP = fresh.nextLabel();
        Var v = jz.v();
        Var tmp = fresh.tempZ();

        out.instructions.add(new IfGoto(jz.label(), v, SKIP, 2)); // if v!=0 goto SKIP
        out.instructions.add(new Inc("", tmp, 1));                 // tmp <- 1
        out.instructions.add(new IfGoto("", tmp, jz.target(), 2)); // goto target
        out.instructions.add(new Nop(SKIP, v, 0));                 // SKIP:
    }
}
