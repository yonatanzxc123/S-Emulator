package system.core.model.synthetic;

import system.core.model.SyntheticInstruction;
import system.core.model.Var;

public final class Assignment implements SyntheticInstruction {
    private final String label;
    private final Var dst;
    private final Var src;

    public Assignment(String label, Var dst, Var src) {
        this.label = label;
        this.dst = dst;
        this.src = src;
    }

    @Override public String label() { return label; }
    @Override public int cycles() { return 4; }                 // appendix
    @Override public String asText() { return dst.asText() + " <- " + src.asText(); }
    public Var dst() { return dst; }
    public Var src() { return src; }
}
