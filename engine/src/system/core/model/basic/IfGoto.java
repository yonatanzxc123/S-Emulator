package system.core.model.basic;

import system.core.model.BasicInstruction;
import system.core.model.Var;

import java.util.List;

public final class IfGoto extends BasicInstruction {
    private final Var v;
    private final String target;

    public IfGoto(String label, Var v, String target, int cycles) {
        super(label, cycles);                  // cycles should be 2 for JNZ
        this.v = v;
        this.target = target == null ? "" : target;
    }

    public Var v()          { return v; }
    public String target()  { return target; }

    @Override
    public String asText() { return "IF " + v + " != 0 GOTO " + target; }

    @Override
    public List<Var> variablesUsed() { return List.of(v); }
}
