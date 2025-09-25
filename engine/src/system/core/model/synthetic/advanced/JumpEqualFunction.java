package system.core.model.synthetic.advanced;

import system.core.exec.*;
import system.core.expand.helpers.FreshNames;
import system.core.io.LoaderUtil;
import system.core.model.*;
import system.core.model.synthetic.JumpEqualVariable;
import system.core.model.synthetic.advanced.helpers.CallSyntax;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * JUMP_EQUAL_FUNCTION
 *
 * Goal:
 *   Jump to label L if variable V equals the result of applying function Q to the
 *   provided arguments.
 *
 * User view:     IF V = Q(x1,…) GOTO L
 * Cycles:        6  (+ inner function runtime when executing at degree 0)
 *
 * XML fields:
 *   varToken (from the <Instruction var="...">)  -> V
 *   functionName                                  -> Q
 *   functionArguments                              -> inner args string (may be "" or have nested calls)
 *   JEFunctionLabel                                -> L
 *
 * Expansion uses a fresh z to hold Q(...) and then a regular "jump if equal" to L.
 */
public final class JumpEqualFunction extends SyntheticInstruction
        implements Remappable, SelfExecutable {

    private final Var v;                   // V
    private final String functionName;     // Q
    /** Inner arguments list (no surrounding parentheses) for Q. */
    private final String functionArguments;
    private final String targetLabel;      // L

    public JumpEqualFunction(String label,
                             Var v,
                             String functionName,
                             String functionArguments,
                             String targetLabel) {
        super(label);
        this.v = v;
        this.functionName = functionName;
        this.functionArguments = (functionArguments == null) ? "" : functionArguments.trim();
        this.targetLabel = (targetLabel == null) ? "" : targetLabel.trim();
    }

    @Override public int cycles() { return 6; }

    @Override
    public String asText() {
        var namer = CallSyntax.envNamerOrIdentity();
        String fnShown = namer.apply(functionName);
        String inner   = CallSyntax.renderInnerArgsPretty(
                CallSyntax.parseArgs(functionArguments),
                namer
        );
        String args = inner.isEmpty() ? "" : "," + inner;
        return "IF " + v + " = (" + fnShown + args + ") GOTO " + targetLabel;
    }



    @Override public List<Var> variablesUsed() { return List.of(v); }

    @Override public List<String> labelTargets() {
        if (targetLabel.isBlank() || "EXIT".equals(targetLabel)) return List.of();
        return List.of(targetLabel);
    }


    // ---------- degree 0: execute directly (evaluate Q(...) then compare) ----------
    @Override
    public void executeSelf(MachineState s, JumpResolver jr) {
        Program prog = FunctionEnv.current().get(functionName);
        if (prog == null) throw new IllegalStateException("Function '" + functionName + "' not found");

        List<Long> xs = new ArrayList<>();
        for (CallSyntax.Arg a : CallSyntax.parseArgs(functionArguments)) xs.add(evalArg(a, s));
        MachineState sub = new Executor().run(prog, xs);

        s.addCycles(this.cycles());
        s.addCycles((int) Math.min(Integer.MAX_VALUE, sub.cycles()));

        if (s.get(v) == sub.y()) {
            // Adjust if your JumpResolver API differs.
            int dest =jr.resolve(targetLabel);
            if(dest == JumpResolver.EXIT) {
                s.halt();
                return;
            }
            if (dest == JumpResolver.NOT_FOUND) {
                throw new IllegalStateException("label '" + targetLabel + "' not found");
            }
            if (dest >= 0){
                s.jumpTo(dest);
                return;

            }

        }

        s.advance();

    }

    private long evalArg(CallSyntax.Arg a, MachineState s) {
        return switch (a) {
            case CallSyntax.VarRef vr -> s.get(vr.v());
            case CallSyntax.Call call -> {
                List<Long> xs = new ArrayList<>();
                for (CallSyntax.Arg sub : call.args()) xs.add(evalArg(sub, s));
                Program subProg = FunctionEnv.current().get(call.name());
                if (subProg == null) throw new IllegalStateException("Function '" + call.name() + "' not found");
                MachineState sub = new Executor().run(subProg, xs);
                yield sub.y();
            }
        };
    }

    // ---------- expansion: use QUOTE to compute into a fresh z, then compare ----------
    @Override
    public void expandTo(Program out, FreshNames fresh) {
        Var z = fresh.tempZ(); // the spec says "use a free z1"; a fresh Z avoids collisions.

        // Keep our original label on the first emitted instruction:
        out.add(new Quote(label(), z, functionName, functionArguments));

        // Now jump if V == z to L. Replace with your project's instruction if named differently.
        out.add(new JumpEqualVariable("", v, z, targetLabel));
    }

    // ---------- loader hook ----------
    public static Instruction fromXml(String label, String varToken, Map<String,String> args, List<String> errs) {
        Var v = LoaderUtil.parseVar(varToken, errs, -1);
        String fn   = LoaderUtil.need(args.get("functionName"), "functionName", -1, errs);
        String farg = args.getOrDefault("functionArguments", "").trim();
        // Keep raw inner-args as-is; callers should not wrap with outer "()"
        String tgt  = LoaderUtil.need(args.get("JEFunctionLabel"), "JEFunctionLabel", -1, errs);
        if (v == null || fn == null || tgt == null) return null;
        return new JumpEqualFunction(label, v, fn, farg, tgt);
    }

    @Override
    public Instruction remap(UnaryOperator<Var> vm, UnaryOperator<String> lm) {
        return new JumpEqualFunction(
                lm.apply(label()),
                vm.apply(v),
                functionName,
                functionArguments,
                lm.apply(targetLabel)
        );
    }
}
