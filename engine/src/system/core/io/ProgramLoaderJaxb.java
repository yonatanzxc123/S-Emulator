// java
package system.core.io;

import jakarta.xml.bind.*;
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

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;

public final class ProgramLoaderJaxb implements ProgramLoader {

    private static void log(String msg) {
        System.err.println("[ProgramLoaderJaxb] " + msg);
    }

    @Override
    public LoadOutcome load(Path xmlPath) {
        long t0 = System.currentTimeMillis();
        var errs = new ArrayList<String>();
        try {
            log("load(file) start: " + xmlPath);
            if (xmlPath == null || !Files.isRegularFile(xmlPath)) {
                String m = "File not found: " + xmlPath;
                log("error: " + m);
                return LoadOutcome.error(List.of(m));
            }
            JAXBContext ctx = JAXBContext.newInstance(ObjectFactory.class);
            Unmarshaller u = ctx.createUnmarshaller();
            log("JAXBContext ready");

            Schema schema = tryLoadSchema(xmlPath);
            List<String> schemaErrs = new ArrayList<>();
            if (schema != null) {
                log("schema attached");
                u.setSchema(schema);
                u.setEventHandler(new ValidationEventHandler() {
                    @Override public boolean handleEvent(ValidationEvent event) {
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
                        String msg = sev + pos + ": " + event.getMessage();
                        log("validation: " + msg);
                        schemaErrs.add(msg);
                        return true; // keep collecting
                    }
                });
            } else {
                log("no schema found (parsing without validation)");
            }

            SProgram root = (SProgram) u.unmarshal(xmlPath.toFile());
            log("unmarshal OK; raw instr count = " +
                    ((root != null && root.getSInstructions() != null && root.getSInstructions().getSInstruction() != null)
                            ? root.getSInstructions().getSInstruction().size() : 0));

            if (!schemaErrs.isEmpty()) {
                log("schema errors: " + String.join(" | ", schemaErrs));
                return LoadOutcome.error(schemaErrs);
            }

            if (root == null || root.getSInstructions() == null || root.getSInstructions().getSInstruction() == null) {
                String m = "Empty or invalid <S-Program>.";
                log("error: " + m);
                return LoadOutcome.error(List.of(m));
            }

            // Build main program
            Program program = mapProgramFrom(
                    root.getSInstructions(),
                    (root.getName() == null ? "Unnamed" : root.getName()),"",
                    errs
            );
            log("main program built: name='" + program.name() + "', instr=" + program.instructions().size());

            // Build functions map
            Map<String, Program> functions = new LinkedHashMap<>();
            if (root.getSFunctions() != null && root.getSFunctions().getSFunction() != null) {
                for (SFunction f : root.getSFunctions().getSFunction()) {
                    String formalName = safe(f.getName());
                    String userString = safe(f.getUserString());
                    if (formalName.isBlank()) {
                        errs.add("Function with empty 'name' attribute.");
                        continue;
                    }
                    if (functions.containsKey(formalName)) {
                        errs.add("Duplicate function name: " + formalName);
                        continue;
                    }
                    if (f.getSInstructions() == null || f.getSInstructions().getSInstruction() == null) {
                        errs.add("Function '" + formalName + "' has no <S-Instructions>.");
                        continue;
                    }
                    Program body = mapProgramFrom(
                            f.getSInstructions(),
                            userString.isBlank() ? formalName : userString,
                            "",
                            errs
                    );
                    functions.put(formalName, body);
                }
            }
            log("functions built: count=" + functions.size());

            // Validate labels in the main program
            labelSanity(program, errs);

            if (!errs.isEmpty()) {
                log("build errors: " + String.join(" | ", errs));
                return LoadOutcome.error(errs);
            }
            long dt = System.currentTimeMillis() - t0;
            log("load(file) OK in " + dt + " ms");
            return LoadOutcome.ok(program, functions);

        } catch (Exception e) {
            e.printStackTrace(System.err);
            Throwable root = e;
            while (root.getCause() != null) root = root.getCause();
            String msg = root.getMessage() != null ? root.getMessage() : root.toString();
            String full = "Parse error: " + root.getClass().getName() + ": " + msg;
            log(full);
            errs.add(full);
            return LoadOutcome.error(errs);
        }
    }

    private static Program mapProgramFrom(SInstructions block, String name, String userString, List<String> errs) {
        Program p = new Program(name);
        int line = 0;
        for (SInstruction si : block.getSInstruction()) {
            line++;
            String kind  = safe(si.getType());
            String nameOp  = safe(si.getName());
            String label = safe(si.getSLabel());
            String var   = safe(si.getSVariable());
            Map<String,String> args = argsMap(si.getSInstructionArguments());

            Instruction ins = buildByConvention(kind, nameOp, label, var, args, line, errs);
            if (ins != null) p.add(ins);
        }
        return p;
    }

    private static void labelSanity(Program program, List<String> errs) {
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
    }

    // ---------- reflection + convention things ----------
    private static final Map<String,String> BASIC_ALIASES = Map.of(
            "INCREASE",      "Inc",
            "DECREASE",      "Dec",
            "JUMP_NOT_ZERO", "IfGoto",
            "NEUTRAL",       "Nop"
    );

    private static Instruction buildByConvention(
            String kind, String name, String label, String varToken,
            Map<String,String> args, int line, List<String> errs
    ) {
        if (name == null || name.isBlank()) {
            errs.add("Missing instruction name at #" + line);
            return null;
        }

        // pick candidate packages
        List<String> pkgs;
        String className;

        if ("basic".equalsIgnoreCase(kind)) {
            if (BASIC_ALIASES.containsKey(name.toUpperCase(Locale.ROOT))) {
                className = BASIC_ALIASES.get(name.toUpperCase(Locale.ROOT));
            } else {
                className = toCamel(name);
            }
            pkgs = List.of("system.core.model.basic");
        } else if ("synthetic".equalsIgnoreCase(kind)) {
            className = toCamel(name);
            pkgs = List.of(
                    "system.core.model.synthetic",
                    "system.core.model.synthetic.advanced"
            );
        } else {
            errs.add("Unknown type '" + kind + "' at #" + line + " (expected 'basic' or 'synthetic').");
            return null;
        }

        Exception lastError = null;
        for (String base : pkgs) {
            String fqcn = base + "." + className;
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
                lastError = e; // try next package
            } catch (NoSuchMethodException e) {
                errs.add("Instruction class " + fqcn + " is missing static fromXml(String,String,Map,List).");
                return null;
            } catch (Exception e) {
                errs.add("Failed to build " + fqcn + " at #" + line + ": " + e.getMessage());
                return null;
            }
        }

        errs.add("Unknown instruction at #" + line + ": '" + name + "' (tried " +
                String.join(", ", pkgs) + " as " + className + ")");
        return null;
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

    // Prefer v2 schema, then fallback to v1; log each attempt
    private static Schema tryLoadSchema(Path xmlPath) {
        try {
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

            if (xmlPath != null && xmlPath.getParent() != null) {
                Path near2 = xmlPath.getParent().resolve("S-Emulator-v2.xsd");
                log("schema lookup (near file): " + near2.toAbsolutePath());
                if (Files.isRegularFile(near2)) {
                    log("using schema: " + near2.toAbsolutePath());
                    return sf.newSchema(near2.toFile());
                }
                Path near1 = xmlPath.getParent().resolve("S-Emulator-v1.xsd");
                log("schema lookup (near file): " + near1.toAbsolutePath());
                if (Files.isRegularFile(near1)) {
                    log("using schema: " + near1.toAbsolutePath());
                    return sf.newSchema(near1.toFile());
                }
            }

            Path root2 = Path.of("S-Emulator-v2.xsd").toAbsolutePath();
            log("schema lookup (cwd): " + root2);
            if (Files.isRegularFile(root2)) {
                log("using schema: " + root2);
                return sf.newSchema(root2.toFile());
            }
            Path root1 = Path.of("S-Emulator-v1.xsd").toAbsolutePath();
            log("schema lookup (cwd): " + root1);
            if (Files.isRegularFile(root1)) {
                log("using schema: " + root1);
                return sf.newSchema(root1.toFile());
            }

            log("schema not found");
        } catch (Exception e) {
            log("schema load failed: " + e);
        }
        return null;
    }

    /** Load directly from XML text (no files) */
    public LoadOutcome loadFromString(String xml) {
        long t0 = System.currentTimeMillis();
        var errs = new ArrayList<String>();
        try {
            log("load(string) start; xml.length=" + (xml == null ? 0 : xml.length()));
            if (xml == null || xml.isBlank()) {
                String m = "Empty XML";
                log("error: " + m);
                return LoadOutcome.error(List.of(m));
            }
            JAXBContext ctx = JAXBContext.newInstance(ObjectFactory.class);
            Unmarshaller u = ctx.createUnmarshaller();
            log("JAXBContext ready");

            Schema schema = tryLoadSchema(null);
            List<String> schemaErrs = new ArrayList<>();
            if (schema != null) {
                log("schema attached");
                u.setSchema(schema);
                u.setEventHandler(event -> {
                    ValidationEventLocator loc = event.getLocator();
                    String pos = (loc != null) ? (" line " + loc.getLineNumber() + ", col " + loc.getColumnNumber()) : "";
                    String sev = switch (event.getSeverity()) {
                        case ValidationEvent.WARNING -> "Warning";
                        case ValidationEvent.ERROR -> "Error";
                        case ValidationEvent.FATAL_ERROR -> "Fatal";
                        default -> "Unknown";
                    };
                    String msg = sev + pos + ": " + event.getMessage();
                    log("validation: " + msg);
                    schemaErrs.add(msg);
                    return true;
                });
            } else {
                log("no schema found (parsing without validation)");
            }

            SProgram root = (SProgram) u.unmarshal(new StreamSource(new StringReader(xml)));
            log("unmarshal OK; raw instr count = " +
                    ((root != null && root.getSInstructions() != null && root.getSInstructions().getSInstruction() != null)
                            ? root.getSInstructions().getSInstruction().size() : 0));

            if (!schemaErrs.isEmpty()) {
                log("schema errors: " + String.join(" | ", schemaErrs));
                return LoadOutcome.error(schemaErrs);
            }

            if (root == null || root.getSInstructions() == null || root.getSInstructions().getSInstruction() == null) {
                String m = "Empty or invalid <S-Program>.";
                log("error: " + m);
                return LoadOutcome.error(List.of(m));
            }

            Program program = mapProgramFrom(
                    root.getSInstructions(),
                    (root.getName() == null ? "Unnamed" : root.getName()),
                    "",
                    errs
            );
            log("main program built: name='" + program.name() + "', instr=" + program.instructions().size());

            Map<String, Program> functions = new LinkedHashMap<>();
            if (root.getSFunctions() != null && root.getSFunctions().getSFunction() != null) {
                for (SFunction f : root.getSFunctions().getSFunction()) {
                    String formalName = safe(f.getName());
                    String userString = safe(f.getUserString());
                    if (formalName.isBlank()) { errs.add("Function with empty 'name' attribute."); continue; }
                    if (functions.containsKey(formalName)) { errs.add("Duplicate function name: " + formalName); continue; }
                    if (f.getSInstructions() == null || f.getSInstructions().getSInstruction() == null) {
                        errs.add("Function '" + formalName + "' has no <S-Instructions>.");
                        continue;
                    }
                    Program body = mapProgramFrom(f.getSInstructions(),
                            userString.isBlank() ? formalName : userString,"",
                            errs);
                    functions.put(formalName, body);
                }
            }
            log("functions built: count=" + functions.size());

            labelSanity(program, errs);
            if (!errs.isEmpty()) {
                log("build errors: " + String.join(" | ", errs));
                return LoadOutcome.error(errs);
            }
            long dt = System.currentTimeMillis() - t0;
            log("load(string) OK in " + dt + " ms");
            return LoadOutcome.ok(program, functions);

        } catch (Exception e) {
            e.printStackTrace(System.err);
            Throwable root = e;
            while (root.getCause() != null) root = root.getCause();
            String msg = root.getMessage() != null ? root.getMessage() : root.toString();
            String full = "Parse error: " + root.getClass().getName() + ": " + msg;
            log(full);
            errs.add(full);
            return LoadOutcome.error(errs);
        }
    }
}
