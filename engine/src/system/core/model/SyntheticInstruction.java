package system.core.model;

public abstract non-sealed class SyntheticInstruction implements Instruction {
    protected final String label;

    protected SyntheticInstruction(String label) {
        this.label = (label == null ? "" : label);
    }

    @Override public String label()   { return label; }
    @Override public boolean isBasic(){ return false; }
    // cycles(), asText() are provided by each concrete synthetic instruction
    // variablesUsed() defaults to Instruction#variablesUsed unless overridden
}
