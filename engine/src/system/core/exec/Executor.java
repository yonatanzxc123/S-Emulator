package system.core.exec;

import system.core.model.Instruction;
import system.core.model.Program;
import system.core.model.basic.*;
import system.core.model.synthetic.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@FunctionalInterface
interface InstrHandler<T extends Instruction> {
    void execute(T ins, MachineState st, JumpResolver jr);
}

public final class Executor {
    private final Map<Class<?>, InstrHandler<?>> handlers = new HashMap<>();


    public Executor() {
        // ===== BASIC INSTRUCTIONS =====

        register(Inc.class, (Inc i, MachineState s, JumpResolver j) -> {
            s.add(i.v(), +1);
            charge(s, i, "");
            s.advance();
        });

        register(Dec.class, (Dec i, MachineState s, JumpResolver j) -> {
            long before = s.get(i.v());
            if (before > 0) s.set(i.v(), before - 1);
            charge(s, i, "before=" + before);
            s.advance();
        });


        register(Nop.class, (Nop i, MachineState s, JumpResolver j) -> {
            charge(s, i, "");
            s.advance();
        });

        // IMPORTANT: correct branch-dependent cycles for IfGoto
        register(IfGoto.class, (IfGoto i, MachineState s, JumpResolver j) -> {
            boolean taken = (s.get(i.v()) != 0);
            charge(s, i, taken ? "TAKEN" : "NOT TAKEN");
            if (taken) {
                int to = j.resolve(i.target());
                if (to == JumpResolver.NOT_FOUND || to == JumpResolver.EXIT) s.halt();
                else s.jumpTo(to);
            } else {
                s.advance();
            }
        });


        // ===== EXISTING SYNTHETICS =====
        // (These remain so degree=0 runs still work today.
        //  New synthetics can just implement SelfExecutable and need no changes here.)

        register(ZeroVariable.class, (ZeroVariable i, MachineState s, JumpResolver j) -> {
            s.set(i.v(), 0);
            charge(s, i, "");
            s.advance();
        });

        register(GotoLabel.class, (GotoLabel i, MachineState s, JumpResolver j) -> {
            int to = j.resolve(i.target());
            charge(s, i, "-> " + to);
            if (to == JumpResolver.NOT_FOUND || to == JumpResolver.EXIT) s.halt();
            else s.jumpTo(to);
        });


        register(Assignment.class, (Assignment i, MachineState s, JumpResolver j) -> {
            s.set(i.v(), s.get(i.src()));
            charge(s, i, "");
            s.advance();
        });

        register(ConstantAssignment.class, (ConstantAssignment i, MachineState s, JumpResolver j) -> {
            s.set(i.v(), i.k());
            charge(s, i, "");
            s.advance();
        });

        // Synthetic conditional jumps: also fix branch-dependent cycles
        register(JumpZero.class, (JumpZero i, MachineState s, JumpResolver j) -> {
           boolean taken = (s.get(i.v()) == 0);
            charge(s, i, taken ? "TAKEN" : "NOT TAKEN");
            if (taken) {
                int to = j.resolve(i.target());
                if (to == JumpResolver.NOT_FOUND || to == JumpResolver.EXIT) s.halt();
                else s.jumpTo(to);
            } else {
                s.advance();
            }
        });

        register(JumpEqualConstant.class, (JumpEqualConstant i, MachineState s, JumpResolver j) -> {
            boolean taken = (s.get(i.v()) == i.k());
            charge(s, i, taken ? "TAKEN" : "NOT TAKEN");
            if (taken) {
                int to = j.resolve(i.target());
                if (to == JumpResolver.NOT_FOUND || to == JumpResolver.EXIT) s.halt();
                else s.jumpTo(to);
            } else {
                s.advance();
            }
        });



        register(JumpEqualVariable.class, (JumpEqualVariable i, MachineState s, JumpResolver j) -> {
            boolean taken = (s.get(i.a()) == s.get(i.b()));
            charge(s, i, taken ? "TAKEN" : "NOT TAKEN");
            if (taken) {
                int to = j.resolve(i.target());
                if (to == JumpResolver.NOT_FOUND || to == JumpResolver.EXIT) s.halt();
                else s.jumpTo(to);
            } else {
                s.advance();
            }
        });

    }

    private <T extends Instruction> void register(Class<T> cls, InstrHandler<T> h) {
        handlers.put(cls, h);
    }

    @SuppressWarnings("unchecked")
    public void step(Instruction ins, MachineState st, JumpResolver jr) {
        // If the instruction chooses to run itself, let it (no executor changes needed)
        if (ins instanceof SelfExecutable se) {
            se.executeSelf(st, jr);
            return;
        }

        var h = (InstrHandler<Instruction>) handlers.get(ins.getClass());
        if (h == null) {
            throw new IllegalStateException("No handler for " + ins.getClass().getName()
                    + " (either add a handler or implement SelfExecutable)");
        }
        h.execute(ins, st, jr);
    }

    public MachineState run(Program p, List<Long> inputs) {
        MachineState st = MachineState.init(inputs);
        JumpResolver jr = LabelIndex.build(p);

        while (!st.isHalted() && st.getPc() < p.instructions().size()) {
            final int pc = st.getPc();                     // pc of the instruction we’re about to execute
            Instruction ins = p.instructions().get(pc);
            step(ins, st, jr);                        // this adds the basic cycles only
        }
        return st;
    }



    // For Debugging / Tracing -> make TRACE false to disable
    private void charge(MachineState s, Instruction i, String note) {
        s.addCycles(i.cycles());
        // Toggle for Debug prints
        boolean TRACE = true;
        if (!TRACE) return;

        // Try to show label and text if available; fall back gracefully
        String label = "";
        try { label = (String) Instruction.class.getMethod("label").invoke(i); } catch (Exception ignore) {}
        String text  = "";
        try { text  = (String) Instruction.class.getMethod("asText").invoke(i); } catch (Exception ignore) {}

        System.out.printf("PC=%d  +%d (total=%d)  %-18s  %s%s%s%n",
                s.getPc(), i.cycles(), s.cycles(),
                i.getClass().getSimpleName(),
                label == null || label.isEmpty() ? "" : ("[" + label + "] "),
                text == null ? "" : text,
                note == null || note.isEmpty() ? "" : ("  // " + note)
        );
    }



}
