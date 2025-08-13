package system.core.expand.sugars;

import system.core.expand.syntheticinterface.SugarExpander;
import system.core.expand.helpers.FreshNames;

import system.core.model.Program;
import system.core.model.Instruction;
import system.core.model.Var;


import system.core.model.basic.Inc;
import system.core.model.basic.IfGoto;

import system.core.model.synthetic.GotoLabel;

public final class GotoLabelExpander implements SugarExpander {
    @Override
    public boolean canHandle(Instruction ins) {
        return ins instanceof GotoLabel;
    }

    @Override
    public void expand(Instruction ins, Program out, FreshNames fresh) {
        GotoLabel gl = (GotoLabel) ins;
        Var tmp = fresh.tempZ();                           // a fresh temp z
        out.instructions.add(new Inc(gl.label(), tmp, 1)); // tmp <- 1
        out.instructions.add(new IfGoto("", tmp, gl.target(), 2)); // goto target
    }
}
