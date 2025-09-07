package system.core.exec;

import system.core.model.Var;

import java.util.HashMap;
import java.util.Map;
import java.util.BitSet;


public final class MachineState {
    private int pc = 0;                 // 0-based index
    private boolean halted = false;
    private long cycles = 0;
    private final BitSet chargedSynthetic = new BitSet();



    private final Map<Integer, Long> x = new HashMap<>(); // x1.. -> value
    private final Map<Integer, Long> z = new HashMap<>(); // z1.. -> value
    private long y = 0;

    private MachineState() {}

    public static MachineState init(java.util.List<Long> inputs) {
        MachineState st = new MachineState();
        if (inputs != null) {
            for (int i = 0; i < inputs.size(); i++) {
                long v = inputs.get(i) == null ? 0L : Math.max(0L, inputs.get(i));
                st.x.put(i + 1, v); // x1.. = inputs
            }
        }
        // y and z* default to 0 as per instruction
        return st;
    }

    public long get(Var v) {
        return switch (v.symbol()) {
            case 'x' -> x.getOrDefault(v.index(), 0L);
            case 'z' -> z.getOrDefault(v.index(), 0L);
            case 'y' -> y;
            default -> 0L;
        };
    }

    public void set(Var v, long value) {
        long val = Math.max(0L, value);
        switch (v.symbol()) {
            case 'x' -> x.put(v.index(), val);
            case 'z' -> z.put(v.index(), val);
            case 'y' -> y = val;
        }
    }

    public void add(Var v, long delta) {
        long nv = get(v) + delta;
        if (nv < 0) nv = 0;
        set(v, nv);
    }

    public void floorAtZero(Var v) {
        long nv = get(v);
        if (nv < 0) set(v, 0);
    }



    public boolean  isHalted() { return halted; }
    public int getPc() { return pc; }

    public void advance() { pc++; }
    public void jumpTo(int index) { pc = index; }
    public void halt() { halted = true; }
    public void addCycles(int c) { cycles += c; }


    // expose snapshots for building RunResult later
    public long y() { return y; }
    public long cycles() { return cycles; }
    public void setPc(int pc) { this.pc = Math.max(0, pc); }
    public void setCycles(long c) { this.cycles = Math.max(0L, c); }


    public Map<Integer, Long> snapshotX() { return new HashMap<>(x); }
    public Map<Integer, Long> snapshotZ() { return new HashMap<>(z); }


    // for Debugger to restore state
    public void restoreX(Map<Integer,Long> src) {
        x.clear();
        if (src != null) x.putAll(src);
    }
    public void restoreZ(Map<Integer,Long> src) {
        z.clear();
        if (src != null) z.putAll(src);
    }


}
