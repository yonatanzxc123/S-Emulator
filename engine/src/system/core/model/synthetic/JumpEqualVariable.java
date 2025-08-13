package system.core.model.synthetic;

import system.core.model.SyntheticInstruction;
import system.core.model.Var;

import java.util.List;

public final class JumpEqualVariable extends SyntheticInstruction {
    private final Var a;
    private final Var b;
    private final String target;

    public JumpEqualVariable(String label, Var a, Var b, String target) {
        super(label);
        this.a = a;
        this.b = b;
        this.target = (target == null ? "" : target);
    }

    public Var a() { return a; }
    public Var b() { return b; }
    public String target() { return target; }

    @Override public int cycles() { return 2; }  // per spec
    @Override public String asText() { return "IF " + a + " = " + b + " GOTO " + target; }
    @Override public List<Var> variablesUsed() { return List.of(a, b); }
}
