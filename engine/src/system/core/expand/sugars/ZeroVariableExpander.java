package system.core.expand.sugars;

import system.core.expand.syntheticinterface.SugarExpander;
import system.core.expand.helpers.FreshNames;

import system.core.model.Program;
import system.core.model.Instruction;
import system.core.model.Var;

import system.core.model.basic.*;
import system.core.model.synthetic.*;

public final class ZeroVariableExpander implements SugarExpander {
    @Override public boolean canHandle(Instruction ins) { return ins instanceof ZeroVariable; }

    @Override
    public void expand(Instruction ins, Program out, FreshNames fresh) {
        ZeroVariable zv = (ZeroVariable) ins;
        String LOOP = fresh.nextLabel();
        Var v = zv.v();
        out.instructions.add(new IfGoto(zv.label(), v, LOOP, 2));
        out.instructions.add(new Dec(LOOP, v, 1));
        out.instructions.add(new IfGoto("", v, LOOP, 2));
    }
}
