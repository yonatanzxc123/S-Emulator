package system.core.io;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;

import jaxb.*; // <-- my generated package (engine/src/jaxb/*.java)

import system.core.model.*;             // Program, Var
import system.core.model.basic.*;       // Inc, Dec, IfGoto, Nop
import system.core.model.synthetic.*;   // ZeroVariable, ...

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class ProgramLoaderJaxb implements ProgramLoader {

    @Override
    public LoadOutcome load(Path xmlPath) {
        var errs = new ArrayList<String>();
        try {
            if (xmlPath == null || !Files.isRegularFile(xmlPath)) {
                return LoadOutcome.error(List.of("File not found: " + xmlPath));
            }

            JAXBContext ctx = JAXBContext.newInstance(ObjectFactory.class);
            Unmarshaller u = ctx.createUnmarshaller();
            SProgram root = (SProgram) u.unmarshal(xmlPath.toFile());

            if (root == null || root.getSInstructions() == null || root.getSInstructions().getSInstruction() == null) {
                return LoadOutcome.error(List.of("Empty or invalid <S-Program>."));
            }

            Program p = new Program(root.getName() == null ? "Unnamed" : root.getName());

            int line = 0;
            for (SInstruction si : root.getSInstructions().getSInstruction()) {
                line++;
                String kind  = safe(si.getType());          // "basic" | "synthetic"
                String name  = safe(si.getName()).toUpperCase(Locale.ROOT);
                String label = safe(si.getSLabel());        // may be "", "L1", or "EXIT"
                String var   = safe(si.getSVariable());     // "x1","z3","y" (some synthetics may ignore)

                Map<String,String> a = argsMap(si.getSInstructionArguments());

                switch (name) {
                    // ===== BASIC =====
                    case "INCREASE" -> {
                        expect(kind, "basic", line, errs);
                        p.instructions.add(new Inc(label, parseVar(var, errs, line), 1));
                    }
                    case "DECREASE" -> {
                        expect(kind, "basic", line, errs);
                        p.instructions.add(new Dec(label, parseVar(var, errs, line), 1));
                    }
                    case "JUMP_NOT_ZERO" -> {
                        expect(kind, "basic", line, errs);
                        String target = need(a.get("JNZLabel"), "JNZLabel", line, errs);
                        p.instructions.add(new IfGoto(label, parseVar(var, errs, line), target, 2));
                    }
                    case "NEUTRAL" -> {
                        expect(kind, "basic", line, errs);
                        p.instructions.add(new Nop(label, parseVar(var, errs, line), 0));
                    }

                    // ===== SYNTHETIC =====
                    case "ZERO_VARIABLE" -> {
                        expect(kind, "synthetic", line, errs);
                        p.instructions.add(new ZeroVariable(label, parseVar(var, errs, line)));
                    }
                    case "GOTO_LABEL" -> {
                        expect(kind, "synthetic", line, errs);
                        String target = need(a.get("gotoLabel"), "gotoLabel", line, errs);
                        p.instructions.add(new GotoLabel(label, target));
                    }
                    case "ASSIGNMENT" -> {
                        expect(kind, "synthetic", line, errs);
                        String src = need(a.get("assignedVariable"), "assignedVariable", line, errs);
                        p.instructions.add(new Assignment(label, parseVar(var, errs, line), parseVar(src, errs, line)));
                    }
                    case "CONSTANT_ASSIGNMENT" -> {
                        expect(kind, "synthetic", line, errs);
                        String kStr = need(a.get("constantValue"), "constantValue", line, errs);
                        long k = parseNonNegLong(kStr, "constantValue", line, errs);
                        p.instructions.add(new ConstantAssignment(label, parseVar(var, errs, line), k));
                    }
                    case "JUMP_ZERO" -> {
                        expect(kind, "synthetic", line, errs);
                        String target = need(a.get("JZLabel"), "JZLabel", line, errs);
                        p.instructions.add(new JumpZero(label, parseVar(var, errs, line), target));
                    }
                    case "JUMP_EQUAL_CONSTANT" -> {
                        expect(kind, "synthetic", line, errs);
                        String target = need(a.get("JEConstantLabel"), "JEConstantLabel", line, errs);
                        String kStr = need(a.get("constantValue"), "constantValue", line, errs);
                        long k = parseNonNegLong(kStr, "constantValue", line, errs);
                        p.instructions.add(new JumpEqualConstant(label, parseVar(var, errs, line), k, target));
                    }
                    case "JUMP_EQUAL_VARIABLE" -> {
                        expect(kind, "synthetic", line, errs);
                        String target = need(a.get("JEVariableLabel"), "JEVariableLabel", line, errs);
                        String other  = need(a.get("variableName"), "variableName", line, errs);
                        p.instructions.add(new JumpEqualVariable(label, parseVar(var, errs, line), parseVar(other, errs, line), target));
                    }

                    default -> errs.add("Unknown instruction name at #" + line + ": '" + name + "'");
                }
            }

            // Validate label references (EXIT is allowed)
            Set<String> defined = new HashSet<>();
            for (var ins : p.instructions) {
                String lab = ins.label();
                if (!lab.isEmpty() && !"EXIT".equals(lab)) defined.add(lab);
            }
            for (int i = 0; i < p.instructions.size(); i++) {
                var ins = p.instructions.get(i);
                String missing = switch (ins) {
                    case IfGoto ig -> checkTarget(ig.target(), defined);
                    case GotoLabel gl -> checkTarget(gl.target(), defined);
                    case JumpZero jz -> checkTarget(jz.target(), defined);
                    case JumpEqualConstant jec -> checkTarget(jec.target(), defined);
                    case JumpEqualVariable jev -> checkTarget(jev.target(), defined);
                    default -> null;
                };
                if (missing != null) errs.add("Unknown label referenced at #" + (i+1) + ": '" + missing + "'");
            }

            if (!errs.isEmpty()) return LoadOutcome.error(errs);
            return LoadOutcome.ok(p);

        } catch (Exception e) {
            errs.add("Parse error: " + e.getMessage());
            return LoadOutcome.error(errs);
        }
    }

    // ---- helpers ----
    private static String safe(String s){ return s==null? "" : s.trim(); }
    private static void expect(String actual, String expected, int line, List<String> errs) {
        if (!expected.equalsIgnoreCase(actual)) {
            errs.add("Instruction #" + line + " expected type='" + expected + "' but got '" + actual + "'");
        }
    }
    private static String need(String v, String name, int line, List<String> errs) {
        if (v == null || v.trim().isEmpty()) { errs.add("Missing @" + name + " at instruction #" + line); return ""; }
        return v.trim();
    }
    private static long parseNonNegLong(String v, String name, int line, List<String> errs) {
        try {
            long k = Long.parseLong(v);
            if (k < 0) { errs.add("@" + name + " must be >= 0 at instruction #" + line); return 0L; }
            return k;
        } catch (Exception ex) {
            errs.add("Bad number for @" + name + " at instruction #" + line + ": '" + v + "'");
            return 0L;
        }
    }
    private static String checkTarget(String target, Set<String> defined) {
        if (target == null || target.isEmpty() || "EXIT".equals(target)) return null;
        return defined.contains(target) ? null : target;
    }
    private static Map<String,String> argsMap(SInstructionArguments args) {
        Map<String,String> m = new HashMap<>();
        if (args != null && args.getSInstructionArgument() != null) {
            for (SInstructionArgument a : args.getSInstructionArgument()) {
                if (a.getName() != null) m.put(a.getName(), a.getValue());
            }
        }
        return m;
    }
    private static Var parseVar(String text, List<String> errs, int line) {
        if (text == null || text.isBlank()) { errs.add("Missing <S-Variable> at instruction #" + line); return Var.z(1); }
        String s = text.trim();
        if (s.equals("y")) return Var.y();
        if (s.startsWith("x")) {
            try { return Var.x(Integer.parseInt(s.substring(1))); }
            catch (NumberFormatException e){ errs.add("Bad x index at #" + line + ": " + s); return Var.x(1); }
        }
        if (s.startsWith("z")) {
            try { return Var.z(Integer.parseInt(s.substring(1))); }
            catch (NumberFormatException e){ errs.add("Bad z index at #" + line + ": " + s); return Var.z(1); }
        }
        errs.add("Unknown variable token at #" + line + ": " + s);
        return Var.z(1);
    }
}
