package system.core.model;

public abstract non-sealed class BasicInstruction implements Instruction {
    protected final String label;
    protected final int cycles;

    protected BasicInstruction(String label, int cycles) {
        this.label = (label == null ? "" : label);
        this.cycles = cycles;
    }

    @Override public String label()   { return label; }
    @Override public int cycles()     { return cycles; }
    @Override public boolean isBasic(){ return true; }
}
