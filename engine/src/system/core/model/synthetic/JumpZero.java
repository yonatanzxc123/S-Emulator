package system.core.model.synthetic;

import system.core.model.SyntheticInstruction;
import system.core.model.Var;

public final class JumpZero implements SyntheticInstruction {
    private final String label;
    private final Var v;
    private final String target;

    public JumpZero(String label, Var v, String target) {
        this.label = label;
        this.v = v;
        this.target = target;
    }

    public Var v() { return v; }
    public String target() { return target; }


    @Override public String label() { return label; }
    @Override public int cycles() { return 2; }                 // appendix
    @Override public String asText() { return "IF " + v.asText() + " = 0 GOTO " + target; }

}
