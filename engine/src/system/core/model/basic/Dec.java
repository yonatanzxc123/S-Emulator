package system.core.model.basic;

import system.core.model.BasicInstruction;
import system.core.model.Var;

public final class Dec implements BasicInstruction {
    private final String label;
    private final Var v;
    private final int cycles;

    public Dec(String label, Var v, int cycles) { this.label = label; this.v = v; this.cycles = cycles; }

    public Var v() { return v; }

    @Override public String label()  { return label; }
    @Override public int cycles()    { return cycles; }
    @Override public String asText() { return v.asText() + " <- " + v.asText() + " - 1"; }
}
