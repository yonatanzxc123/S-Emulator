package system.core.expand.sugars;

import system.core.expand.syntheticinterface.SugarExpander;
import system.core.expand.helpers.FreshNames;

import system.core.model.Program;
import system.core.model.Instruction;
import system.core.model.Var;
import system.core.model.basic.*;
import system.core.model.synthetic.*;

public final class ConstantAssignmentExpander implements SugarExpander {

    @Override
    public boolean canHandle(Instruction ins) {
        return ins instanceof ConstantAssignment;
    }

    @Override
    public void expand(Instruction ins, Program out, FreshNames fresh) {
        ConstantAssignment ca = (ConstantAssignment) ins;
        Var v = ca.v();      // make sure ConstantAssignment has v() cuz then we are in bad shape
        long k = ca.k();     // and k() of course

        // First zero V (leave it synthetic to be expanded in next degree if needed for me)
        out.instructions.add(new ZeroVariable(ca.label(), v));

        // Then add K increments
        for (long i = 0; i < k; i++) {
            out.instructions.add(new Inc("", v, 1));
        }
    }
}
