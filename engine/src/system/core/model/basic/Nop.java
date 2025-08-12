package system.core.model.basic;

import system.core.model.BasicInstruction;
import system.core.model.Var;

public final class Nop implements BasicInstruction {
    private final String label; private final Var v; private final int cycles;

    public Nop(String label, Var v, int cycles){ this.label=label; this.v=v; this.cycles=cycles; }
    public String label(){ return label; }
    public int cycles(){ return cycles; }
    public String asText(){ return v.asText() + " <- " + v.asText(); }
    public Var v(){ return v;}

}
