package system.core.model.synthetic;

import system.core.model.SyntheticInstruction;
import system.core.model.Var;   // if the class uses Var check later


public final class JumpEqualConstant implements SyntheticInstruction {
    private final String label;
    private final String target;
    private final long k;
    private final system.core.model.Var v;

    public JumpEqualConstant(String label, system.core.model.Var v, long k, String target) {
        this.label = label;
        this.v = v;
        this.k = k;
        this.target = target;
    }

    @Override public String label() { return label; }
    @Override public int cycles() { return 2; }                 // appendix
    @Override public String asText() { return "IF " + v.asText() + " = " + k + " GOTO " + target; }

    public system.core.model.Var v() { return v; }
    public long k() { return k; }
    public String target() { return target;  }  // for the assembler

}
