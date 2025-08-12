package system.core.model.synthetic;

import system.core.model.SyntheticInstruction;
import system.core.model.Var;   // if the class uses Var, check later


public final class GotoLabel implements SyntheticInstruction {
    private final String label; private final String target;
    public GotoLabel(String label, String target){ this.label=label; this.target=target; }
    public String label(){ return label; }
    public int cycles(){ return 1; }
    public String asText(){ return "GOTO " + target; }
    public String target(){ return target;  }  // for the assembler
}
