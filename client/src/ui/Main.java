package ui;

import system.api.EmulatorEngine;
import system.api.RunResult;
import system.api.view.ProgramView;
import system.api.view.CommandView;
import system.core.EmulatorEngineImpl;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.HashSet;





public final class Main {
    public static void main(String[] args) {
        EmulatorEngine engine = new EmulatorEngineImpl();
        Scanner in = new Scanner(System.in);

        System.out.println("S-Emulator");
        while (true) {
            // need to swap Exit and Load\Save Idan
            System.out.println(""" 
                1) Load XML
                2) Show Program
                3) Expand & Show Program
                4) Run  Program
                5) Show History
                6) Exit
                7) Save State
                8) Load State
                9) (hidden) Debug
                """);
            System.out.print("Choose: ");
            String choice = in.nextLine().trim();

            switch (choice) {
                case "1" -> {
                    System.out.print("Enter full path to .xml: ");
                    String p = in.nextLine().trim().replace("\"","");
                    var res = engine.loadProgram(Path.of(p));
                    if (res.ok()) {
                        System.out.println("Loaded OK. Version=" + engine.getVersion());
                    } else {
                        System.out.println("Load failed:");
                        res.errors().forEach(err -> System.out.println("  - " + err));
                    }
                }
                case "2" -> {
                    ProgramView v = engine.getProgramView();
                    if (v == null) {
                        System.out.println("No program loaded yet. Use option 1 first.");
                        break;
                    }
                    System.out.println("Program: " + v.name());
                    if (!v.inputsUsed().isEmpty())
                        System.out.println("Inputs: " + String.join(", ", v.inputsUsed()));
                    if (!v.labelsInOrder().isEmpty())
                        System.out.println("Labels: " + String.join(", ", v.labelsInOrder()));

                    for (CommandView c : v.commands()) {
                        String line = String.format(
                                "#%d (%s) [%-5s] %s (%d)",
                                c.number(),
                                c.basic() ? "B" : "S",
                                c.labelOrEmpty() == null ? "" : c.labelOrEmpty(),
                                c.text(),
                                c.cycles()
                        );
                        System.out.println(line);
                    }

                }

                case "3" -> {
                    var v = engine.getProgramView();
                    if (v == null) { System.out.println("No program loaded yet. Use option 1 first."); break; }

                    int max = engine.getMaxDegree();
                    System.out.println("Max expandable degree: " + max);
                    System.out.print("Expand to degree (0.." + max + "): ");
                    String s = in.nextLine().trim();
                    int degree;
                    try { degree = Integer.parseInt(s); } catch (NumberFormatException e) { degree = 0; }
                    if (degree > max) {
                        System.out.println("Requested degree exceeds max. Using " + max + " instead.");
                        degree = max;
                    } else if (degree < 0) {
                        degree = 0;
                    }

                    ProgramView pv = engine.getExpandedProgramView(degree);
                    System.out.println("Program (expanded to degree " + degree + "): " + pv.name());
                    if (!pv.inputsUsed().isEmpty())
                        System.out.println("Inputs: " + String.join(", ", pv.inputsUsed()));
                    if (!pv.labelsInOrder().isEmpty())
                        System.out.println("Labels: " + String.join(", ", pv.labelsInOrder()));

                    for (var c : pv.commands()) {
                        String line = String.format(
                                "#%d (%s) [%-5s] %s (%d)",
                                c.number(), c.basic() ? "B" : "S",
                                c.labelOrEmpty() == null ? "" : c.labelOrEmpty(),
                                c.text(), c.cycles()
                        );
                        if (c.originChain() != null && !c.originChain().isBlank()) {
                            line = line + "  >>>  " + c.originChain();
                        }
                        System.out.println(line);
                    }

                }

                case "4" -> {
                    int degree = -1;
                    ProgramView preview = engine.getExpandedProgramView(degree);
                    if (preview == null) { System.out.println("No program loaded yet. Use option 1 first."); break; }
                    int max = engine.getMaxDegree();
                    System.out.println("Max expandable degree: " + max);

                    while (true) {
                        System.out.print("Choose degree [0.." + max + "]: ");
                        String d = in.nextLine().trim();
                        try { degree = Integer.parseInt(d); } catch (NumberFormatException e) { degree = -1; }
                        if (degree >= 0 && degree <= max) break;

                        System.out.println("Invalid degree. The maximum possible is " + max + ".");
                        System.out.print("Use max (" + max + ")? [Y/n]: ");
                        String ans = in.nextLine().trim().toLowerCase();
                        if (ans.isEmpty() || ans.startsWith("y")) { degree = max; break; }
                    }

                    // Show program header BEFORE collecting inputs
                    System.out.println("Program (will run at degree " + degree + "): " + preview.name());
                    if (!preview.inputsUsed().isEmpty())
                        System.out.println("Inputs: " + String.join(", ", preview.inputsUsed()));
                    if (!preview.labelsInOrder().isEmpty())
                        System.out.println("Labels: " + String.join(", ", preview.labelsInOrder()));

                    // Now collect inputs from the user
                    System.out.print("Inputs (comma separated, e.g. 1,2,3): ");
                    String line = in.nextLine().trim();
                    List<Long> inputs = new ArrayList<>();
                    if (!line.isEmpty()) {
                        for (String s : line.split(",")) {
                            try { inputs.add(Long.parseLong(s.trim())); }
                            catch (NumberFormatException e) { inputs.add(0L); }
                        }
                    }

                    RunResult runResult = engine.run(degree, inputs);
                    if (runResult == null) {
                        System.out.println("No program loaded yet. Use option 1 first.");
                        break;
                    }

                    // Print executed program (same format as Expand), including origins
                    System.out.println("Executed program:");
                    ProgramView pv = runResult.executedProgram();
                    for (var c : pv.commands()) {
                        String ln = String.format(
                                "#%d (%s) [%-5s] %s (%d)",
                                c.number(), c.basic() ? "B" : "S",
                                c.labelOrEmpty() == null ? "" : c.labelOrEmpty(),
                                c.text(), c.cycles()
                        );
                        System.out.println(ln + (c.originChain().isEmpty() ? "" : "  >>>  " + c.originChain()));
                    }

                    // Variables & cycles
                    runResult.variablesOrdered().forEach((k,v) -> System.out.println(k + " = " + v));
                    System.out.println("cycles = " + runResult.cycles());
                }
                case "5" -> {
                    var hist = engine.getRunHistory();
                    if (hist.isEmpty()) {
                        System.out.println("No runs yet.");
                    } else {
                        for (var h : hist) {
                            String inputsStr = h.inputs().isEmpty()
                                    ? "[]"
                                    : "[" + h.inputs().toString().replaceAll("[\\[\\]]","") + "]";
                            System.out.printf("#%d  degree=%d  inputs=%s  y=%d  cycles=%d%n",
                                    h.runNo(), h.degree(), inputsStr, h.y(), h.cycles());
                        }
                    }
                }
                case "6" -> { System.out.println("Bye."); return; }


                case "7" -> { //
                    Path p = askPath(in, "Enter full path (without extension) to SAVE: ");
                    var res = engine.saveState(p);
                    if (res.ok()) {
                        System.out.println("State saved (file will have .state extension).");
                    } else {
                        System.out.println("Save failed:");
                        res.errors().forEach(System.out::println);
                    }
                }

                case "8" -> {
                    Path p = askPath(in, "Enter full path (without extension) to LOAD: ");
                    var res = engine.loadState(p);
                    if (res.ok()) {
                        System.out.println("State loaded successfully.");
                        // Optional: quick summary
                        System.out.println("Version: " + engine.getVersion());
                        var pv = engine.getProgramView();
                        System.out.println(pv == null ? "No program in state." : "Program: " + pv.name());
                        System.out.println("History entries: " + engine.getRunHistory().size());
                    } else {
                        System.out.println("Load failed:");
                        res.errors().forEach(System.out::println);
                    }


                }
                // this case is for testing debugging feature, will later be implemented in GUI
                case "9" -> {
                    var preview = engine.getProgramView();
                    if (preview == null) { System.out.println("No program loaded yet. Use option 1 first."); break; }

                    int max = engine.getMaxDegree();
                    System.out.println("Max expandable degree: " + max);
                    int degree = -1;
                    while (true) {
                        System.out.print("Choose degree [0.." + max + "]: ");
                        String d = in.nextLine().trim();
                        try { degree = Integer.parseInt(d); } catch (NumberFormatException e) { degree = -1; }
                        if (degree >= 0 && degree <= max) break;
                        System.out.println("Invalid degree. Try again.");
                    }

                    System.out.print("Inputs (comma separated, e.g. 1,2,3; empty for none): ");
                    String line = in.nextLine().trim();
                    List<Long> inputs = new ArrayList<>();
                    if (!line.isEmpty()) {
                        for (String s : line.split(",")) {
                            try { inputs.add(Long.parseLong(s.trim())); } catch (NumberFormatException e) { inputs.add(0L); }
                        }
                    }

                    var dbg = engine.startDebug(degree, inputs);
                    if (dbg == null) { System.out.println("Failed to start debugger (no program loaded)."); break; }

                    var debugProgramView = system.core.io.ProgramMapper.toView(dbg.program());

                    // Ask for breakpoints (1-based)
                    System.out.print("Breakpoints (line numbers comma-separated; empty = none): ");
                    String bpLine = in.nextLine().trim();
                    if (!bpLine.isEmpty()) {
                        Set<Integer> bps = new HashSet<>();
                        for (String tok : bpLine.split(",")) {
                            try {
                                int ln = Integer.parseInt(tok.trim());
                                if (ln >= 1 && ln <= debugProgramView.commands().size()) {
                                    bps.add(ln - 1); // convert to 0-based PC
                                } else {
                                    System.out.println("Ignoring invalid line " + ln);
                                }
                            } catch (NumberFormatException ignore) {}
                        }
                        dbg.setBreakpoints(bps);
                    }

                    // capture-safe copies for lambda
                    final int dbgDegree = degree;
                    final var dbgPV     = debugProgramView;

                    java.util.function.Consumer<system.api.DebugStep> printState = step -> {
                        System.out.println("DEBUG - Program (degree " + dbgDegree + "): " + dbgPV.name());
                        System.out.println("PC=" + step.pc() + "  cycles=" + step.cycles() + "  finished=" + step.finished());

                        for (var c : dbgPV.commands()) {
                            int pc = c.number() - 1;
                            String bpFlag = dbg.isBreakpoint(pc) ? "*" : " "; // show '*' on lines with a breakpoint
                            String cursor = (pc == step.pc() && !step.finished()) ? ">>" : "  ";
                            String ln = String.format(
                                    "%s%s #%d (%s) [%-5s] %s (%d)",
                                    bpFlag, cursor,
                                    c.number(), c.basic() ? "B" : "S",
                                    c.labelOrEmpty() == null ? "" : c.labelOrEmpty(),
                                    c.text(), c.cycles()
                            );
                            System.out.println(ln);
                        }

                        System.out.println("-- VARIABLES (full snapshot) --");
                        step.vars().forEach((k,v) -> System.out.println(k + " = " + v));

                        if (!step.changed().isEmpty()) {
                            System.out.println("-- CHANGED in last step --");
                            step.changed().forEach((k,v) -> System.out.println("* " + k + " = " + v));
                        }
                    };

                    // If we have any breakpoints, immediately run to the first one
                    var cur = dbg.getBreakpoints().isEmpty() ? dbg.peek() : dbg.resume();
                    printState.accept(cur);

                    debugLoop:
                    while (true) {
                        System.out.println("""
        Debug commands:
          s = step
          b = step back
          r = resume
          x = stop
          a N = add breakpoint at line N
          d N = delete breakpoint at line N
          q = quit debug
        """);
                        System.out.print("[debug] command: ");
                        String cmd = in.nextLine().trim();

                        if (cmd.equalsIgnoreCase("q")) { System.out.println("Leaving debug."); break debugLoop; }
                        else if (cmd.equalsIgnoreCase("s")) cur = dbg.step();
                        else if (cmd.equalsIgnoreCase("b")) cur = dbg.stepBack();
                        else if (cmd.equalsIgnoreCase("r")) cur = dbg.resume();
                        else if (cmd.equalsIgnoreCase("x")) cur = dbg.stop();
                        else if (cmd.matches("^[aA]\\s+\\d+$")) {
                            int ln = Integer.parseInt(cmd.substring(1).trim());
                            int pc = ln - 1;
                            dbg.addBreakpoint(pc);
                            System.out.println("Added breakpoint at line " + ln);
                        } else if (cmd.matches("^[dD]\\s+\\d+$")) {
                            int ln = Integer.parseInt(cmd.substring(1).trim());
                            int pc = ln - 1;
                            dbg.removeBreakpoint(pc);
                            System.out.println("Removed breakpoint at line " + ln);
                        } else {
                            System.out.println("Unknown command.");
                            continue;
                        }

                        printState.accept(cur);
                        if (cur.finished()) {
                            System.out.println("Program finished. You can 'b' to walk back or 'q' to exit debug.");
                        }
                    }
                }





                default -> System.out.println("Invalid option.");
            }
        }
    }
    //Helper for Save/Load state (options 6,7)
    private static Path askPath(Scanner sc, String prompt) {
        System.out.print(prompt);
        String input = sc.nextLine().trim();
        while (input.isEmpty()) {
            System.out.print("Path can't be empty. Try again: ");
            input = sc.nextLine().trim();
        }
        return Paths.get(input);
    }


}
