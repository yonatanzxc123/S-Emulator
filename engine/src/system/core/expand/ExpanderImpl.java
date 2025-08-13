package system.core.expand;

import system.core.model.Program;
import system.core.model.Instruction;
import system.core.model.Var;
import system.core.model.basic.Dec;
import system.core.model.basic.IfGoto;
import system.core.model.basic.Inc;
import system.core.model.basic.Nop;
import system.core.model.synthetic.*;

import java.util.concurrent.atomic.AtomicInteger;

public final class ExpanderImpl implements Expander {

    @Override
    public Program expandToDegree(Program program, int degree) {
        if (program == null || degree <= 0) return program;
        Program cur = program;
        for (int d = 1; d <= degree; d++) {
            cur = expandOne(cur);
        }
        return cur;
    }

    private Program expandOne(Program p) {
        Program out = new Program(p.name + " [expanded]");
        AtomicInteger fresh = new AtomicInteger(1);

        for (Instruction ins : p.instructions) {
            if (ins instanceof ZeroVariable zv) {
                expandZero(zv, out, fresh);
            } else if (ins instanceof GotoLabel gl) {
                expandGoto(gl, out, fresh);
            } else if (ins instanceof ConstantAssignment ca) {
                expandConstAssign(ca, out, fresh);
            } else if (ins instanceof Assignment asg) {
                expandAssignment(asg, out, fresh);
            } else if (ins instanceof JumpZero jz) {
                expandJumpZero(jz, out, fresh);
            } else {
                // leave basic or not-yet-supported synthetic as-is
                out.instructions.add(ins);
            }
        }
        return out;
    }

    private static String L(AtomicInteger fresh) { return "L$" + fresh.getAndIncrement(); }

    /** ZERO_VARIABLE: V <- 0  (loop: while v != 0 { v-- }) */
    private void expandZero(ZeroVariable zv, Program out, AtomicInteger fresh) {
        String LOOP = L(fresh);
        Var v = zv.v();
        out.instructions.add(new IfGoto(zv.label(), v, LOOP, 2)); // if v!=0 goto LOOP; else fall through
        out.instructions.add(new Dec(LOOP, v, 1));                // LOOP: v<-v-1
        out.instructions.add(new IfGoto("", v, LOOP, 2));         // if v!=0 goto LOOP
    }

    /** GOTO_LABEL: uncond. jump via temp z1 */
    private void expandGoto(GotoLabel gl, Program out, AtomicInteger fresh) {
        Var z1 = Var.z(1);
        out.instructions.add(new Inc(gl.label(), z1, 1));         // z1 <- z1+1
        out.instructions.add(new IfGoto("", z1, gl.target(), 2)); // if z1!=0 goto target
    }

    /** CONSTANT_ASSIGNMENT: V <- K  (zero then K increments) */
    private void expandConstAssign(ConstantAssignment ca, Program out, AtomicInteger fresh) {
        // zero v
        expandZero(new ZeroVariable(ca.label(), ca.v()), out, fresh);
        // add K times
        for (int i = 0; i < ca.k(); i++) {
            out.instructions.add(new Inc("", ca.v(), 1));
        }
    }

    /** ASSIGNMENT: V <- V' using temp z1; preserves V' */
    private void expandAssignment(Assignment asg, Program out, AtomicInteger fresh) {
        Var dst = asg.v();
        Var src = asg.src();
        Var tmp = Var.z(1);

        // Ensure tmp = 0
        expandZero(new ZeroVariable(asg.label(), tmp), out, fresh);

        // Zero dst
        expandZero(new ZeroVariable("", dst), out, fresh);

        // Move src -> tmp (destroy src)
        String L1 = L(fresh);
        out.instructions.add(new IfGoto("", src, L1, 2)); // if src!=0 goto L1 else skip
        String L2 = L(fresh); // restore loop head
        out.instructions.add(new IfGoto("", tmp, L2, 2)); // fallthrough to restore when tmp!=0
        // L1: consume src => tmp++
        out.instructions.add(new Dec(L1, src, 1));
        out.instructions.add(new Inc("", tmp, 1));
        out.instructions.add(new IfGoto("", src, L1, 2));

        // L2: restore src and fill dst from tmp
        out.instructions.add(new IfGoto(L2, tmp, L2, 2)); // if tmp!=0 goto L2
        // when tmp==0: done (falls through)
        // L2 body:
        out.instructions.add(new Dec(L2, tmp, 1));
        out.instructions.add(new Inc("", src, 1));
        out.instructions.add(new Inc("", dst, 1));
        out.instructions.add(new IfGoto("", tmp, L2, 2));
    }

    /** JUMP_ZERO: IF V = 0 GOTO L  (if v!=0 skip; else force uncond goto) */
    private void expandJumpZero(JumpZero jz, Program out, AtomicInteger fresh) {
        String SKIP = L(fresh);
        Var v = jz.v();
        Var z1 = Var.z(1);

        out.instructions.add(new IfGoto(jz.label(), v, SKIP, 2)); // if v!=0 goto SKIP
        out.instructions.add(new Inc("", z1, 1));                 // z1 <- 1
        out.instructions.add(new IfGoto("", z1, jz.target(), 2)); // goto target
        out.instructions.add(new Nop(SKIP, v, 0));                // SKIP:
    }
}
