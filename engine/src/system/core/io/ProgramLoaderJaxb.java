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

import system.core.model.Instruction;
import system.core.model.Program;

import java.lang.reflect.Method;
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
                String name  = safe(si.getName());          // e.g., JUMP_ZERO
                String label = safe(si.getSLabel());        // may be "", "L1", or "EXIT"
                String var   = safe(si.getSVariable());     // for ops that use it
                Map<String,String> args = argsMap(si.getSInstructionArguments());

                Instruction ins = buildByConvention(kind, name, label, var, args, line, errs);
                if (ins != null) program.add(ins);
            }

            // Label check via Instruction#labelTargets()
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

    // ---------- reflection + convention ----------

    private static final Map<String,String> BASIC_ALIASES = Map.of(
            // your XML opcode  -> class name
            "INCREASE",        "Inc",
            "DECREASE",        "Dec",
            "JUMP_NOT_ZERO",   "IfGoto",
            "NEUTRAL",         "Nop"
    );

    private static Instruction buildByConvention(
            String kind, String name, String label, String varToken,
            Map<String,String> args, int line, List<String> errs
    ) {
        if (name == null || name.isBlank()) {
            errs.add("Missing instruction name at #" + line);
            return null;
        }
        String pkg = switch (kind.toLowerCase(Locale.ROOT)) {
            case "basic"     -> "system.core.model.basic";
            case "synthetic" -> "system.core.model.synthetic";
            default          -> {
                errs.add("Unknown type '" + kind + "' at #" + line + " (expected 'basic' or 'synthetic').");
                yield null;
            }
        };
        if (pkg == null) return null;

        String className;
        if ("basic".equalsIgnoreCase(kind) && BASIC_ALIASES.containsKey(name.toUpperCase(Locale.ROOT))) {
            className = BASIC_ALIASES.get(name.toUpperCase(Locale.ROOT));
        } else {
            className = toCamel(name); // e.g., JUMP_EQUAL_CONSTANT -> JumpEqualConstant
        }

        String fqcn = pkg + "." + className;
        try {
            Class<?> cls = Class.forName(fqcn);
            if (!Instruction.class.isAssignableFrom(cls)) {
                errs.add("Class " + fqcn + " does not implement Instruction (at #" + line + ").");
                return null;
            }
            Method m = cls.getDeclaredMethod("fromXml", String.class, String.class, Map.class, List.class);
            Object result = m.invoke(null, label, varToken, args, errs);
            return (Instruction) result;
        } catch (ClassNotFoundException e) {
            errs.add("Unknown instruction at #" + line + ": '" + name + "' (not found as " + fqcn + ")");
            return null;
        } catch (NoSuchMethodException e) {
            errs.add("Instruction class " + fqcn + " is missing static fromXml(String,String,Map,List).");
            return null;
        } catch (Exception e) {
            errs.add("Failed to build " + fqcn + " at #" + line + ": " + e.getMessage());
            return null;
        }
    }

    private static String toCamel(String opcodeUpper) {
        String s = opcodeUpper.trim().toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder();
        boolean up = true;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '_' || c == '-' || c == ' ') { up = true; continue; }
            sb.append(up ? Character.toUpperCase(c) : c);
            up = false;
        }
        return sb.toString();
    }

    // ---------- tiny helpers ----------

    private static String safe(String s){ return s==null? "" : s.trim(); }

    private static Map<String,String> argsMap(SInstructionArguments args) {
        Map<String,String> m = new HashMap<>();
        if (args != null && args.getSInstructionArgument() != null) {
            for (SInstructionArgument a : args.getSInstructionArgument()) {
                if (a.getName() != null) m.put(a.getName(), a.getValue());
            }
        }
        return m;
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
}
