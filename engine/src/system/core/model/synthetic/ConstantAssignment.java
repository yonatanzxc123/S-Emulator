package system.core.model.synthetic;

import system.core.model.SyntheticInstruction;
import system.core.model.Var;

public final class ConstantAssignment extends SyntheticInstruction {
    private final Var v;
    private final long k;

    public ConstantAssignment(String label, Var v, long k) {
        super(label);
        this.v = v;
        this.k = k;
    }

    public Var v() { return v; }
    public long k() { return k; }

    @Override public int cycles() { return 2; }
    @Override public String asText() { return v + " <- " + k; }
    @Override public java.util.List<Var> variablesUsed() { return java.util.List.of(v); }
}
