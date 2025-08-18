package system.core.io;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.ValidationEvent;
import jakarta.xml.bind.ValidationEventHandler;
import jakarta.xml.bind.ValidationEventLocator;
import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import jaxb.*; // generated JAXB classes

import system.core.model.*;             // Program, Var, Instruction
import system.core.model.basic.*;       // Inc, Dec, IfGoto, Nop
import system.core.model.synthetic.*;   // ZeroVariable, GotoLabel, ...

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.Locale;

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

            // Optional XSD validation (collect all events, fail after unmarshal)
            Schema schema = tryLoadSchema(xmlPath);
            List<String> schemaErrs = new ArrayList<>();
            if (schema != null) {
                u.setSchema(schema);
                u.setEventHandler(new ValidationEventHandler() {
                    @Override
                    public boolean handleEvent(ValidationEvent event) {
                        ValidationEventLocator loc = event.getLocator();
                        String pos = (loc != null)
                                ? (" line " + loc.getLineNumber() + ", col " + loc.getColumnNumber())
                                : "";
                        String sev = switch (event.getSeverity()) {
                            case ValidationEvent.WARNING -> "Warning";
                            case ValidationEvent.ERROR -> "Error";
                            case ValidationEvent.FATAL_ERROR -> "Fatal";
                            default -> "Unknown";
                        };
                        schemaErrs.add(sev + pos + ": " + event.getMessage());
                        return true; // keep collecting
                    }
                });
            }

            SProgram root = (SProgram) u.unmarshal(xmlPath.toFile());
            if (!schemaErrs.isEmpty()) return LoadOutcome.error(schemaErrs);

            if (root == null || root.getSInstructions() == null || root.getSInstructions().getSInstruction() == null) {
                return LoadOutcome.error(List.of("Empty or invalid <S-Program>."));
            }

            Program program = new Program(root.getName() == null ? "Unnamed" : root.getName());

            int line = 0;
            for (SInstruction si : root.getSInstructions().getSInstruction()) {
                line++;
                String kind  = safe(si.getType());          // "basic" | "synthetic"
                String name  = safe(si.getName());
                String label = safe(si.getSLabel());
                String var   = safe(si.getSVariable());

                Map<String,String> args = argsMap(si.getSInstructionArguments());

                // Build via registry (no switch!)
                Instruction ins = Parsers.build(kind, name, label, var, args, line, errs);
                if (ins != null) {
                    program.add(ins);
                }
            }

            // Generic label validation using Instruction#labelTargets()
            Set<String> defined = new HashSet<>();
            for (var ins : program.instructions()) {
                String lab = ins.label();
                if (!lab.isEmpty() && !"EXIT".equals(lab)) defined.add(lab);
            }
            for (int i = 0; i < program.instructions().size(); i++) {
                var ins = program.instructions().get(i);
                for (String t : ins.labelTargets()) {
                    if (!"EXIT".equals(t) && !defined.contains(t)) {
                        errs.add("Unknown label referenced at #" + (i + 1) + ": '" + t + "'");
                    }
                }
            }

            if (!errs.isEmpty()) return LoadOutcome.error(errs);
            return LoadOutcome.ok(program);

        } catch (Exception e) {
            errs.add("Parse error: " + e.getMessage());
            return LoadOutcome.error(errs);
        }
    }

    // ---------- helpers (kept local to this file) ----------

    private static String safe(String s){ return s==null? "" : s.trim(); }

    private static String need(String v, String name, int line, List<String> errs) {
        if (v == null || v.trim().isEmpty()) {
            errs.add("Missing @" + name + " at instruction #" + line);
            return "";
        }
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

    private static Schema tryLoadSchema(Path xmlPath) {
        try {
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            if (xmlPath != null && xmlPath.getParent() != null) {
                Path near = xmlPath.getParent().resolve("S-Emulator-v1.xsd");
                if (Files.isRegularFile(near)) return sf.newSchema(near.toFile());
            }
            Path root = Path.of("S-Emulator-v1.xsd");
            if (Files.isRegularFile(root)) return sf.newSchema(root.toFile());
        } catch (Exception ignore) {}
        return null;
    }

    // ---------- tiny name->factory registry (lives inside this file) ----------

    private static final class Parsers {
        @FunctionalInterface
        interface Builder {
            Instruction build(String label, String varToken, Map<String,String> args, int line, List<String> errs);
        }
        private static final class Entry {
            final String expectedType;  // "basic" | "synthetic"
            final Builder builder;
            Entry(String expectedType, Builder builder) {
                this.expectedType = expectedType; this.builder = builder;
            }
        }
        private static final Map<String, Entry> REG = new HashMap<>();

        static Instruction build(String kind, String name, String label, String varToken,
                                 Map<String,String> args, int line, List<String> errs) {
            Entry e = REG.get(name == null ? "" : name.toUpperCase(Locale.ROOT));
            if (e == null) {
                errs.add("Unknown instruction name at #" + line + ": '" + name + "'");
                return null;
            }
            if (!e.expectedType.equalsIgnoreCase(kind)) {
                errs.add("Instruction #" + line + " expected type='" + e.expectedType + "' but got '" + kind + "'");
                // still attempt build to surface more issues
            }
            return e.builder.build(label, varToken, args, line, errs);
        }

        private static void reg(String keywordUpper, String expectedType, Builder b) {
            REG.put(keywordUpper, new Entry(expectedType, b));
        }

        static {
            // ===== BASIC =====
            reg("INCREASE", "basic", (label, var, a, line, errs) ->
                    new Inc(label, parseVar(var, errs, line), 1));

            reg("DECREASE", "basic", (label, var, a, line, errs) ->
                    new Dec(label, parseVar(var, errs, line), 1));

            reg("JUMP_NOT_ZERO", "basic", (label, var, a, line, errs) -> {
                String target = need(a.get("JNZLabel"), "JNZLabel", line, errs);
                return new IfGoto(label, parseVar(var, errs, line), target, 2);
            });

            reg("NEUTRAL", "basic", (label, var, a, line, errs) ->
                    new Nop(label, parseVar(var, errs, line), 0));

            // ===== SYNTHETIC =====
            reg("ZERO_VARIABLE", "synthetic", (label, var, a, line, errs) ->
                    new ZeroVariable(label, parseVar(var, errs, line)));

            reg("GOTO_LABEL", "synthetic", (label, var, a, line, errs) -> {
                String target = need(a.get("gotoLabel"), "gotoLabel", line, errs);
                return new GotoLabel(label, target);
            });

            reg("ASSIGNMENT", "synthetic", (label, var, a, line, errs) -> {
                String src = need(a.get("assignedVariable"), "assignedVariable", line, errs);
                return new Assignment(label, parseVar(var, errs, line), parseVar(src, errs, line));
            });

            reg("CONSTANT_ASSIGNMENT", "synthetic", (label, var, a, line, errs) -> {
                String kStr = need(a.get("constantValue"), "constantValue", line, errs);
                long k = parseNonNegLong(kStr, "constantValue", line, errs);
                return new ConstantAssignment(label, parseVar(var, errs, line), k);
            });

            reg("JUMP_ZERO", "synthetic", (label, var, a, line, errs) -> {
                String target = need(a.get("JZLabel"), "JZLabel", line, errs);
                return new JumpZero(label, parseVar(var, errs, line), target);
            });

            reg("JUMP_EQUAL_CONSTANT", "synthetic", (label, var, a, line, errs) -> {
                String target = need(a.get("JEConstantLabel"), "JEConstantLabel", line, errs);
                String kStr = need(a.get("constantValue"), "constantValue", line, errs);
                long k = parseNonNegLong(kStr, "constantValue", line, errs);
                return new JumpEqualConstant(label, parseVar(var, errs, line), k, target);
            });

            reg("JUMP_EQUAL_VARIABLE", "synthetic", (label, var, a, line, errs) -> {
                String target = need(a.get("JEVariableLabel"), "JEVariableLabel", line, errs);
                String other  = need(a.get("variableName"), "variableName", line, errs);
                return new JumpEqualVariable(label, parseVar(var, errs, line), parseVar(other, errs, line), target);
            });
        }
    }
}
