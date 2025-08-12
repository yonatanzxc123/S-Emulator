package system.core.model.synthetic;

import system.core.model.SyntheticInstruction;
import system.core.model.Var;

public final class ConstantAssignment implements SyntheticInstruction {
    private final String label;
    private final Var dst;
    private final long k;

    public ConstantAssignment(String label, Var dst, long k) {
        this.label = label;
        this.dst = dst;
        this.k = k;
    }

    @Override public String label() { return label; }
    @Override public int cycles() { return 2; }                 // appendix
    @Override public String asText() { return dst.asText() + " <- " + k; }
    public Var dst() { return dst; }
    public long k() { return k; }
}
