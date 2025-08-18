package system.core.model.synthetic;

import system.core.model.SyntheticInstruction;
import system.core.model.Var;
import system.core.model.Program;
import system.core.model.basic.*;
import system.core.expand.helpers.FreshNames;

public final class ZeroVariable extends SyntheticInstruction {
    private final Var v;
    public ZeroVariable(String label, Var v) { super(label); this.v = v; }
    public Var v() { return v; }

    @Override public int cycles() { return 1; }
    @Override public String asText() { return v + " <- 0"; }
    @Override public java.util.List<Var> variablesUsed() { return java.util.List.of(v); }

    @Override
    public void expandTo(Program out, FreshNames fresh) {
        String LOOP = fresh.nextLabel();
        out.add(new IfGoto(label(), v, LOOP, 2));  // if v!=0 goto LOOP
        out.add(new Dec(LOOP, v, 1));              // LOOP: v <- v-1
        out.add(new IfGoto("", v, LOOP, 2));       // while v!=0
    }



}


