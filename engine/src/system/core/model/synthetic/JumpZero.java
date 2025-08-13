package system.core.model.synthetic;

import system.core.model.SyntheticInstruction;
import system.core.model.Var;

public final class JumpZero extends SyntheticInstruction {
    private final Var v;
    private final String target;
    public JumpZero(String label, Var v, String target) { super(label); this.v = v; this.target = target; }
    public Var v() { return v; }
    public String target() { return target; }
    @Override public int cycles() { return 2; }
    @Override public String asText() { return "IF " + v + " = 0 GOTO " + target; }
    @Override public java.util.List<Var> variablesUsed() { return java.util.List.of(v); }
}
