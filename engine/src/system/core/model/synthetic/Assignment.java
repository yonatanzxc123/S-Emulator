package system.core.model.synthetic;

import system.core.model.SyntheticInstruction;
import system.core.model.Var;

public final class Assignment extends SyntheticInstruction {
    private final Var v;     // destination
    private final Var src;   // source

    public Assignment(String label, Var v, Var src) {
        super(label);
        this.v = v;
        this.src = src;
    }

    public Var v()   { return v; }
    public Var src() { return src; }

    @Override public int cycles() { return 4; }
    @Override public String asText() { return v + " <- " + src; }
    @Override public java.util.List<Var> variablesUsed() { return java.util.List.of(v, src); }

}
