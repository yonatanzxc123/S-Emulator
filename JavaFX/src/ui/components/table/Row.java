package ui.components.table;

public final class Row {
    private final int line;
    private final String bs;
    private final String label;
    private final String instruction;
    private final int cycles;

    public Row(int line, String bs, String label, String instruction, int cycles) {
        this.line = line;
        this.bs = bs;
        this.label = label;
        this.instruction = instruction;
        this.cycles = cycles;
    }
    public int getLine() { return line; }
    public String getBs() { return bs; }
    public String getLabel() { return label; }
    public String getInstruction() { return instruction; }
    public int getCycles() { return cycles; }
}
