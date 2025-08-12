package system.core.exec;

import system.core.model.Instruction;
import system.core.model.Program;
import system.core.model.Var;
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
        // === basics ===
        register(Inc.class, (Inc i, MachineState s, JumpResolver j) -> {
            s.add(i.v(), +1); s.addCycles(i.cycles()); s.advance();
        });
        register(Dec.class, (Dec i, MachineState s, JumpResolver j) -> {
            long v = s.get(i.v());
            if (v > 0) s.set(i.v(), v - 1);
            s.addCycles(i.cycles()); s.advance();
        });
        register(Nop.class, (Nop i, MachineState s, JumpResolver j) -> {
            s.addCycles(i.cycles()); s.advance();
        });
        register(IfGoto.class, (IfGoto i, MachineState s, JumpResolver j) -> {
            s.addCycles(i.cycles());
            if (s.get(i.v()) != 0) {
                int to = j.resolve(i.target());
                if (to == -1) s.halt(); else s.jumpTo(to);
            } else {
                s.advance();
            }
        });

        // === synthetics (direct effects per appendix) ===
        register(ZeroVariable.class, (ZeroVariable i, MachineState s, JumpResolver j) -> {
            s.set(i.v(), 0); s.addCycles(i.cycles()); s.advance();
        });
        register(GotoLabel.class, (GotoLabel i, MachineState s, JumpResolver j) -> {
            s.addCycles(i.cycles());
            int to = j.resolve(i.target());
            if (to == -1) s.halt(); else s.jumpTo(to);
        });
        register(Assignment.class, (Assignment i, MachineState s, JumpResolver j) -> {
            s.set(i.dst(), s.get(i.src())); s.addCycles(i.cycles()); s.advance();
        });
        register(ConstantAssignment.class, (ConstantAssignment i, MachineState s, JumpResolver j) -> {
            s.set(i.dst(), i.k()); s.addCycles(i.cycles()); s.advance();
        });
        register(JumpZero.class, (JumpZero i, MachineState s, JumpResolver j) -> {
            s.addCycles(i.cycles());
            if (s.get(i.v()) == 0) {
                int to = j.resolve(i.target());
                if (to == -1) s.halt(); else s.jumpTo(to);
            } else s.advance();
        });
        register(JumpEqualConstant.class, (JumpEqualConstant i, MachineState s, JumpResolver j) -> {
            s.addCycles(i.cycles());
            if (s.get(i.v()) == i.k()) {
                int to = j.resolve(i.target());
                if (to == -1) s.halt(); else s.jumpTo(to);
            } else s.advance();
        });
        register(JumpEqualVariable.class, (JumpEqualVariable i, MachineState s, JumpResolver j) -> {
            s.addCycles(i.cycles());
            if (s.get(i.a()) == s.get(i.b())) {
                int to = j.resolve(i.target());
                if (to == -1) s.halt(); else s.jumpTo(to);
            } else s.advance();
        });
    }

    private <T extends Instruction> void register(Class<T> cls, InstrHandler<T> h) {
        handlers.put(cls, h);
    }

    @SuppressWarnings("unchecked")
    public void step(Instruction ins, MachineState st, JumpResolver jr) {
        var h = (InstrHandler<Instruction>) handlers.get(ins.getClass());
        if (h == null) throw new IllegalStateException("No handler for " + ins.getClass().getName());
        h.execute(ins, st, jr);
    }

    public MachineState run(Program p, List<Long> inputs) {
        MachineState st = MachineState.init(inputs);
        JumpResolver jr = LabelIndex.build(p);

        while (!st.halted && st.pc < p.instructions.size()) {
            Instruction ins = p.instructions.get(st.pc);
            step(ins, st, jr);
        }
        return st;
    }
}
