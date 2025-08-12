package system.core.model.synthetic;

import system.core.model.SyntheticInstruction;
import system.core.model.Var;

public final class ZeroVariable implements SyntheticInstruction {
    private final String label;
    private final Var v;

    public ZeroVariable(String label, Var v) {
        this.label = label;
        this.v = v;
    }


    public Var v() { return v; }

    @Override public String label()  { return label; }
    @Override public int cycles()    { return 1; }
    @Override public String asText() { return v.asText() + " <- 0"; }
}
