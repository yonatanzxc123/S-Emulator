package system.core.model;

public sealed interface Instruction
        permits BasicInstruction, SyntheticInstruction {

    String label();   // "", "L7", or "EXIT"
    int cycles();     // per appendix
    boolean isBasic();
    String asText();  // exact display string
}
