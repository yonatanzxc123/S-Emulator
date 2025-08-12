package system.core.model.basic;

import system.core.model.BasicInstruction;
import system.core.model.Var;

public final class IfGoto implements BasicInstruction {
    private final String label;
    private final Var v;
    private final String target;
    private final int cycles;

    public IfGoto(String label, Var v, String target, int cycles) {
        this.label = label; this.v = v; this.target = target; this.cycles = cycles;
    }


    public Var v() { return v; }
    public String target() { return target; }

    @Override public String label()  { return label; }
    @Override public int cycles()    { return cycles; }
    @Override public String asText() { return "IF " + v.asText() + " != 0 GOTO " + target; }
}
