package system.core.model.synthetic;

import system.core.model.SyntheticInstruction;
import system.core.model.Var;
import system.core.model.Program;
import system.core.model.basic.*;
import system.core.expand.helpers.FreshNames;

import java.util.List;


public final class JumpZero extends SyntheticInstruction {
    private final Var v;
    private final String target;
    public JumpZero(String label, Var v, String target) { super(label); this.v = v; this.target = target; }
    public Var v() { return v; }
    public String target() { return target; }


    @Override public int cycles() { return 2; }
    @Override public String asText() { return "IF " + v + " = 0 GOTO " + target; }
    @Override public List<Var> variablesUsed() { return java.util.List.of(v); }

    @Override public List<String> labelTargets() {
        return ("EXIT".equals(target) || target.isBlank()) ? List.of() : List.of(target);
    }

    @Override
    public void expandTo(Program out, FreshNames fresh) {
        String SKIP = fresh.nextLabel();
        Var tmp = fresh.tempZ();
        out.add(new IfGoto(label(), v, SKIP, 2));   // if v!=0 goto SKIP
        out.add(new Inc("", tmp, 1));               // tmp <- 1
        out.add(new IfGoto("", tmp, target, 2));    // goto target
        out.add(new Nop(SKIP, v, 0));               // SKIP:
    }

}
