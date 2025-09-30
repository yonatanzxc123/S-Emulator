package system.core.exec.debugg;

import system.api.DebugStep;
import system.core.exec.*;
import system.core.model.Program;
import system.core.model.Var;

import java.util.*;


/**
 * Debugger  = Executor  +  state & snapshots :)
 * Currently will use Exectuor.step(...) for semnatics, will onlyy manage state/trails.
 */
public class Debugger extends Executor {

    private Program program;
    private JumpResolver jr;
    private MachineState st;
    private int stepNo = 0; // number of completed steps

    // snapshots[0] = initial state (before any step)
    // snapshots[k] = state after k steps (k == stepNo)
    private final List<Snapshot> snapshots = new ArrayList<>();

    // breakpoints
    private final Set<Integer> breakpoints = new HashSet<>();

    // Hold a FunctionEnv to resolve functions in case of Quote instructions for the whole debugg session
    private FunctionEnv env;


    // ------------ public API ------------

    /** Start a new debug session */
    public void init(Program p, List<Long> inputs, Map<String, Program> functions) {
        this.program = Objects.requireNonNull(p, "program");
        this.env = new FunctionEnv(functions);
        this.jr = LabelIndex.build(p);
        this.st = MachineState.init(inputs);
        this.stepNo = 0;
        this.snapshots.clear();
        this.snapshots.add(takeSnapshot(st)); // step 0 snapshot
        this.breakpoints.clear();
    }

    /** Non-destructive view of current state (useful to draw UI at start so take it IDAN) */
    public DebugStep peek() {
        return FunctionEnv.with(env, () -> {
            ensureSession();
            return toStep(stepNo, Map.of());
        });
    }

    /** Execute exactly one instruction (also known as step over). */
    public DebugStep step() {
        return FunctionEnv.with(env, () -> {
            ensureSession();
            if (isFinished()) {
                return toStep(stepNo, Map.of());
            }
            Snapshot before = snapshots.get(stepNo);

            // execute one instruction using the normal Executor semantics
            int pc = st.getPc();
            var ins = program.instructions().get(pc);
            super.step(ins, st, jr); // advances pc / cycles / writes vars

            // record new state
            Snapshot after = takeSnapshot(st);
            snapshots.add(after);
            stepNo++;

            Map<String,Long> changed = diff(before , after);
            return toStep(stepNo, changed);
        });
    }
    /** Run to completion or until next PC is at breakpoint (or until halted by a supreme being such as Carmi) from the current point . */
    public DebugStep resume() {
        return FunctionEnv.with(env, () -> {
            ensureSession();

            // Always execute at least one step first
            if (!isFinished()) {
                step();
            }

            // Then continue until we hit a breakpoint or finish
            while (!isFinished() && !isBreakpoint(st.getPc())) {
                step();
            }

            return toStep(stepNo, Map.of());
        });
    }

    /** Stop immediately (mark halted -> hold up wait a min). */
    public DebugStep stop() {
        return FunctionEnv.with(env, () -> {
            ensureSession();
            st.halt();
            // replace last snapshot with the halted state for consistency
            snapshots.set(stepNo, takeSnapshot(st));
            return toStep(stepNo, Map.of());
        });
    }

    /** Step one instruction backward, if possible if not we cry. */
    public DebugStep stepBack() {
        return FunctionEnv.with(env, () -> {
            ensureSession();
            if (stepNo == 0) {
                return toStep(0, Map.of()); // already at start
            }
            Snapshot erased = snapshots.get(stepNo);       // state after the step we undo
            Snapshot prev = snapshots.get(stepNo - 1);   // state we restore to

            // Restore machine state from prev
            this.st = rehydrate(prev);

            // Drop the erased tail and move the cursor back one step
            snapshots.remove(snapshots.size() - 1);
            stepNo--;

            // Changes to highlight = what just changed due to undo (due + undo = ubadandu?)
            Map<String, Long> changed = diff(prev, erased); // same vars but reversed direction
            return toStep(stepNo, changed);
        });
    }

    public boolean isFinished() {
        return FunctionEnv.with(env, () -> {
            ensureSession();
            return st.isHalted() || st.getPc() >= program.instructions().size();
        });
    }

    public Program program() { return program; } // in case UI needs it if not delete it Idan

    // ------------ internals ------------

    private void ensureSession() {
        if (program == null || st == null || env == null) {
            throw new IllegalStateException("Debugger not initialized. Call init(program, inputs) first.");
        }
    }

    private record Snapshot(
            int pc, boolean halted, long cycles, long y,
            Map<Integer,Long> xs, Map<Integer,Long> zs
    ) {}

    private Snapshot takeSnapshot(MachineState s) {
        return new Snapshot(
                s.getPc(),
                s.isHalted(),
                s.cycles(),
                s.y(),
                s.snapshotX(), // defensive copies are returned by MachineState
                s.snapshotZ()
        );
    }

    /** Build a MachineState from a snapshot (fresh instance). */
    private MachineState rehydrate(Snapshot sn) {
        MachineState r = MachineState.init(List.of()); // start empty
        // x
        for (var e : sn.xs.entrySet()) {
            r.set(Var.x(e.getKey()), e.getValue());
        }
        // z
        for (var e : sn.zs.entrySet()) {
            r.set(Var.z(e.getKey()), e.getValue());
        }
        // y
        r.set(Var.y(), sn.y);

        // cycles / pc / halted
        if (sn.cycles > 0) r.addCycles((int)Math.min(Integer.MAX_VALUE, sn.cycles)); // cycles is long; addCycles(int) in your impl
        if (sn.pc >= 0) r.jumpTo(sn.pc);
        if (sn.halted) r.halt();
        return r;
    }

    /** Full snapshot map like RunResult.variablesOrdered() but as a plain map. */
    private Map<String,Long> flatVars(Snapshot s) {
        var out = new TreeMap<String,Long>((a,b) -> {
            // y first, then x#, then z#
            if (a.equals(b)) return 0;
            if (a.equals("y")) return -1;
            if (b.equals("y")) return 1;
            boolean ax = a.startsWith("x"), bx = b.startsWith("x");
            boolean az = a.startsWith("z"), bz = b.startsWith("z");
            if (ax && bz) return -1; if (az && bx) return 1;
            if (ax && bx) return Integer.compare(
                    Integer.parseInt(a.substring(1)), Integer.parseInt(b.substring(1)));
            if (az && bz) return Integer.compare(
                    Integer.parseInt(a.substring(1)), Integer.parseInt(b.substring(1)));
            return a.compareTo(b);
        });

        out.put("y", s.y);

        for (var e : s.xs.entrySet()) out.put("x" + e.getKey(), e.getValue());
        for (var e : s.zs.entrySet()) out.put("z" + e.getKey(), e.getValue());
        return out;
    }

    /** Diff between two snapshots: variables with different values (after vs before). */
    private Map<String,Long> diff(Snapshot before, Snapshot after) {
        Map<String,Long> changed = new LinkedHashMap<>();
        // y
        if (before.y != after.y) changed.put("y", after.y);

        // union of keys
        var keysX = new HashSet<Integer>();
        keysX.addAll(before.xs.keySet());
        keysX.addAll(after.xs.keySet());
        for (int k : keysX) {
            long bv = before.xs.getOrDefault(k, 0L);
            long av = after.xs.getOrDefault(k, 0L);
            if (bv != av) changed.put("x" + k, av);
        }

        var keysZ = new HashSet<Integer>();
        keysZ.addAll(before.zs.keySet());
        keysZ.addAll(after.zs.keySet());
        for (int k : keysZ) {
            long bv = before.zs.getOrDefault(k, 0L);
            long av = after.zs.getOrDefault(k, 0L);
            if (bv != av) changed.put("z" + k, av);
        }
        return changed;
    }

    private DebugStep toStep(int stepNo, Map<String,Long> changed) {
        Snapshot cur = snapshots.get(stepNo);
        return new DebugStep(
                stepNo,
                cur.pc,
                cur.halted || cur.pc >= program.instructions().size(),
                cur.cycles,
                flatVars(cur),
                changed
        );
    }


    // ------------ breakpoints ------------

    /** Programmatic breakpoint control (UI will call these). */
    public void setBreakpoints(Collection<Integer> pcs) {
        FunctionEnv.with(env, () -> {
            ensureSession();
            breakpoints.clear();
            if (pcs != null) {
                for (int pc : pcs) if (pc >= 0 && pc < program.instructions().size()) breakpoints.add(pc);
            }
            return null;
        });
    }
    public void addBreakpoint(int pc) {
        FunctionEnv.with(env, () -> {
            ensureSession();
            if (pc >= 0 && pc < program.instructions().size()) breakpoints.add(pc);
            return null;
        });
    }
    public void removeBreakpoint(int pc) {
        FunctionEnv.with(env, () -> {
            ensureSession();
            breakpoints.remove(pc);
            return null;
        });
    }
    public void clearBreakpoints() {
        FunctionEnv.with(env, () -> {
            ensureSession();
            breakpoints.clear();
            return null;
        });
    }
    public boolean isBreakpoint(int pc) {
        return breakpoints.contains(pc);
    }
    public Set<Integer> getBreakpoints() {
        return Collections.unmodifiableSet(breakpoints);
    }
}








