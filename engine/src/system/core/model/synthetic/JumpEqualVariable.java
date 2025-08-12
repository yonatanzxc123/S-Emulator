package system.core.model.synthetic;

import system.core.model.SyntheticInstruction;
import system.core.model.Var;

public final class JumpEqualVariable implements SyntheticInstruction {
    private final String label;
    private final Var a;
    private final Var b;
    private final String target;

    public JumpEqualVariable(String label, Var a, Var b, String target) {
        this.label = label;
        this.a = a;
        this.b = b;
        this.target = target;
    }

    @Override public String label() { return label; }
    @Override public int cycles() { return 2; }                 // appendix
    @Override public String asText() { return "IF " + a.asText() + " = " + b.asText() + " GOTO " + target; }

    public Var a() { return a; }
    public Var b() { return b; }
    public String target() { return target;  }  // for the assembler
}
