package system.core.model.synthetic;

import system.core.model.SyntheticInstruction;
import system.core.model.Var;

import java.util.List;

public final class JumpEqualConstant extends SyntheticInstruction {
    private final Var v;
    private final long k;
    private final String target;

    public JumpEqualConstant(String label, Var v, long k, String target) {
        super(label);
        this.v = v;
        this.k = k;
        this.target = (target == null ? "" : target);
    }

    public Var v()        { return v; }
    public long k()       { return k; }
    public String target(){ return target; }

    @Override public int cycles() { return 2; }  // per spec
    @Override public String asText() { return "IF " + v + " = " + k + " GOTO " + target; }
    @Override public List<Var> variablesUsed() { return List.of(v); }
}
