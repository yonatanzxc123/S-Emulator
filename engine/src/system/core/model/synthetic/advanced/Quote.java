package system.core.model.synthetic.advanced;

import system.core.exec.*;
import system.core.expand.helpers.FreshNames;
import system.core.io.LoaderUtil;
import system.core.model.*;
import system.core.model.basic.Nop;
import system.core.model.synthetic.Assignment;
import system.core.model.synthetic.advanced.helpers.CallSyntax;

import java.util.*;
import java.util.function.UnaryOperator;

public final class Quote extends SyntheticInstruction
        implements Remappable, SelfExecutable {

    private final Var target;
    private final String functionName;
    /** Inner arguments list, no surrounding parentheses. Examples: "", "x1,y", "x1,(+,y)". */
    private final String functionArguments;

    public Quote(String label, Var target, String functionName, String functionArguments) {
        super(label);
        this.target = Objects.requireNonNull(target);
        this.functionName = Objects.requireNonNull(functionName);
        this.functionArguments = (functionArguments == null) ? "" : functionArguments.trim();
    }

    @Override public int cycles() { return 5; }

    @Override
    public String asText() {
        var namer = CallSyntax.envNamerOrIdentity();
        String fnShown = namer.apply(functionName);
        String inner   = CallSyntax.renderInnerArgsPretty(
                CallSyntax.parseArgs(functionArguments),
                namer
        );
        String args = inner.isEmpty() ? "" : "," + inner;
        return target + " ← (" + fnShown + args + ")";
    }

    @Override
    public List<Var> variablesUsed() {
        // The target is written; reads come from the argument expressions. take notice
        List<Var> used = new ArrayList<>();
        for (CallSyntax.Arg a : CallSyntax.parseArgs(functionArguments)) collectVars(a, used);
        return used;
    }

    @Override
    public Instruction remap(UnaryOperator<Var> vm, UnaryOperator<String> lm) {
        // Remap vars inside the argument AST too, then re-render inner-args.
        List<CallSyntax.Arg> ast = CallSyntax.parseArgs(functionArguments);
        List<CallSyntax.Arg> remapped = remapArgs(ast, vm);
        String newInner = CallSyntax.renderInnerArgs(remapped);
        return new Quote(lm.apply(label()), vm.apply(target), functionName, newInner);
    }


    // ---------- run-time (SelfExecutable) ----------
    @Override
    public void executeSelf(MachineState s, JumpResolver jr) {
        List<Long> xs = new ArrayList<>();
        for (CallSyntax.Arg a : CallSyntax.parseArgs(functionArguments)) xs.add(evalArg(a, s));
        Program q = requireFunction(functionName);
        Executor ex = new Executor();
        MachineState sub = ex.run(q, xs);

        s.addCycles(this.cycles());
        s.addCycles((int)Math.min(Integer.MAX_VALUE, sub.cycles()));
        s.set(target, sub.y());
        s.advance();
    }

    private long evalArg(CallSyntax.Arg a, MachineState s) {
        return switch (a) {
            case CallSyntax.VarRef vr -> s.get(vr.v());
            case CallSyntax.Call call -> {
                List<Long> xs = new ArrayList<>();
                for (CallSyntax.Arg sub : call.args()) xs.add(evalArg(sub, s));
                // recursive call uses FunctonEnv to find the function by name
                // and Executor to run it with the given arguments
                // then returns the resulting y value
                // (this is a direct eval, not an expansion)
                // (note: this can recurse arbitrarily deep)
                // (also note: this does NOT modify the current MachineState s except for cycles and target assignment)
                // (each nested call gets its own fresh MachineState)
                // (also note: if the function is not found, an exception is thrown)
                // (also note: cycles from the nested call are added to the current state)
                //  yes there are many notes, deal with it
                Program subProg = requireFunction(call.name());
                Executor ex = new Executor();
                MachineState sub = ex.run(subProg, xs);


                // add the cycles from the sub-call to the current state
                s.addCycles((int)Math.min(Integer.MAX_VALUE, sub.cycles()));

                yield sub.y();
            }
        };
    }

    private static Program requireFunction(String name) {
        Program p = FunctionEnv.current().get(name);
        if (p == null) throw new IllegalStateException("Function '" + name + "' not found");
        return p;
    }

    // ---------- expansion ----------
    @Override
    public void expandTo(Program out, FreshNames fresh) {
        Program q = requireFunction(functionName);
        List<Instruction> qBody = q.instructions();

        List<CallSyntax.Arg> args = CallSyntax.parseArgs(functionArguments);

        Map<Integer,Var> xToZ = new HashMap<>();
        Map<Integer,Var> zToZ = new HashMap<>();
        Var zy = fresh.tempZ();
        Set<String> qLabels = new LinkedHashSet<>();

        int maxQz = 0, maxQx = 0;
        for (Instruction ins : qBody) {
            for (Var v : ins.variablesUsed()) {
                if (v.isX()) maxQx = Math.max(maxQx, v.index());
                if (v.isZ()) maxQz = Math.max(maxQz, v.index());
            }
            if (ins.label() != null && !ins.label().isEmpty()) qLabels.add(ins.label());
            for (String t : ins.labelTargets()) if (!"EXIT".equals(t)) qLabels.add(t);
        }

        int needX = Math.max(maxQx, args.size());
        for (int i = 1; i <= needX; i++) xToZ.put(i, fresh.tempZ());
        for (int j = 1; j <= maxQz; j++) zToZ.put(j, fresh.tempZ());

        Map<String,String> labMap = new HashMap<>();
        for (String L : qLabels) labMap.put(L, fresh.nextLabel());
        final String Lend = fresh.nextLabel();

        List<Instruction> seq = new ArrayList<>();

        // keep the QUOTE's original label (blue bar)
        seq.add(new Nop(label(), Var.y(), 0));

        // compute each argument into xToZ[i]
        for (int i = 0; i < args.size(); i++) {
            Var dst = xToZ.get(i + 1);
            compileArg(args.get(i), dst, seq, fresh);
        }

        // remap Q body: x_i -> xToZ[i], z_j -> fresh z, y -> zy, labels -> fresh (EXIT -> Lend)
        UnaryOperator<Var> vm = v -> {
            if (v.isX()) return xToZ.getOrDefault(v.index(), v);
            if (v.isZ()) return zToZ.getOrDefault(v.index(), v);
            if (v.isY()) return zy;
            return v;
        };
        UnaryOperator<String> lm = lab -> {
            if (lab == null || lab.isEmpty()) return lab;
            return "EXIT".equals(lab) ? Lend : labMap.getOrDefault(lab, lab);
        };
        for (Instruction ins : qBody) {
            seq.add(((Remappable)ins).remap(vm, lm));
        }

        // Lend: target <- zy
        seq.add(new Assignment(Lend, target, zy));

        seq.forEach(out::add);
    }

    private static String renderArgsOnly(List<CallSyntax.Arg> args) {
        return CallSyntax.renderInnerArgs(args);
    }

    /** emit arg expression into dst; nested calls become nested QUOTEs */
    private void compileArg(CallSyntax.Arg a, Var dst, List<Instruction> out, FreshNames fresh) {
        if (a instanceof CallSyntax.VarRef vr) {
            out.add(new Assignment("", dst, vr.v()));
        } else {
            CallSyntax.Call call = (CallSyntax.Call) a;
            String fargs = CallSyntax.renderInnerArgs(call.args());
            out.add(new Quote("", dst, call.name(), fargs));
        }
    }

    // ---------- loader hook ----------
    public static Instruction fromXml(String label, String varToken, Map<String,String> args, List<String> errs) {
        Var dst   = LoaderUtil.parseVar(varToken, errs, -1);
        String fn = LoaderUtil.need(args.get("functionName"), "functionName", -1, errs);
        String fargs = args.getOrDefault("functionArguments", "").trim();
        if (dst == null || fn == null) return null;
        return new Quote(label, dst, fn, fargs);
    }



    //helpers


    private static void collectVars(CallSyntax.Arg a, List<Var> out) {
        if (a instanceof CallSyntax.VarRef vr) out.add(vr.v());
        else {
            CallSyntax.Call c = (CallSyntax.Call) a;
            for (CallSyntax.Arg sub : c.args()) collectVars(sub, out);
        }
    }

    private static List<CallSyntax.Arg> remapArgs(List<CallSyntax.Arg> args, java.util.function.UnaryOperator<Var> vm) {
        List<CallSyntax.Arg> out = new ArrayList<>(args.size());
        for (CallSyntax.Arg a : args) {
            if (a instanceof CallSyntax.VarRef vr) {
                out.add(new CallSyntax.VarRef(vm.apply(vr.v())));
            } else {
                CallSyntax.Call c = (CallSyntax.Call) a;
                out.add(new CallSyntax.Call(c.name(), remapArgs(c.args(), vm)));
            }
        }
        return out;
    }

}
