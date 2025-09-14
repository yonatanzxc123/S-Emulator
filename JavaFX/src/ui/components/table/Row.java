package ui.components.table;

import system.api.view.CommandView;

public final class Row {
    private final CommandView cmd;
    private final int displayLine;


    public Row(CommandView cmd) {
        this(cmd, cmd.number());
    }


    public Row(CommandView cmd, int displayLine) {
        this.cmd = cmd;
        this.displayLine = displayLine;
    }


    public int getLine() { return displayLine; }
    public String getBs() { return cmd.basic() ? "B" : "S"; }
    public String getLabel() { return cmd.labelOrEmpty() == null ? "" : cmd.labelOrEmpty(); }
    public String getInstruction() { return cmd.text(); }
    public int getCycles() { return cmd.cycles(); }


    public CommandView getCommand() { return cmd; }
}
