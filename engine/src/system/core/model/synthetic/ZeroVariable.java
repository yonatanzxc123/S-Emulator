package system.core.model.synthetic;

import system.core.model.SyntheticInstruction;
import system.core.model.Var;

public final class ZeroVariable extends SyntheticInstruction {
    private final Var v;
    public ZeroVariable(String label, Var v) { super(label); this.v = v; }
    public Var v() { return v; }
    @Override public int cycles() { return 1; }
    @Override public String asText() { return v + " <- 0"; }
    @Override public java.util.List<Var> variablesUsed() { return java.util.List.of(v); }
}


