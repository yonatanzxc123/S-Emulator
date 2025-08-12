package ui;

import system.api.EmulatorEngine;
import system.api.RunResult;
import system.api.view.ProgramView;
import system.api.view.CommandView;
import system.core.EmulatorEngineImpl;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public final class Main {
    public static void main(String[] args) {
        EmulatorEngine engine = new EmulatorEngineImpl();
        Scanner in = new Scanner(System.in);

        System.out.println("S-Emulator");
        while (true) {
            System.out.println("""
                1) Load XML
                2) Show Program
                6) Exit
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
                    // Print header info
                    System.out.println("Program: " + v.name());
                    if (!v.inputsUsed().isEmpty())
                        System.out.println("Inputs: " + String.join(", ", v.inputsUsed()));
                    if (!v.labelsInOrder().isEmpty())
                        System.out.println("Labels: " + String.join(", ", v.labelsInOrder()));

                    // Print instructions in format
                    for (CommandView c : v.commands()) {
                        String line = String.format(
                                "#%d (%s) [%-5s] %s (%d)",
                                c.number(), c.basic() ? "B" : "S",
                                c.labelOrEmpty() == null ? "" : c.labelOrEmpty(),
                                c.text(), c.cycles()
                        );
                        System.out.println(line + (c.originChain().isEmpty() ? "" : "  <<<  " + c.originChain()));
                    }
                }
                case "4" -> {
                    System.out.print("Degree (0 for now): ");
                    String d = in.nextLine().trim();
                    int degree = 0;
                    try { degree = Integer.parseInt(d); } catch (NumberFormatException ignored) {}

                    System.out.print("Inputs (comma separated, e.g. 1,2,3): ");
                    String line = in.nextLine().trim();
                    java.util.List<Long> inputs = new java.util.ArrayList<>();
                    if (!line.isEmpty()) {
                        for (String s : line.split(",")) {
                            try { inputs.add(Long.parseLong(s.trim())); }
                            catch (NumberFormatException e) { inputs.add(0L); }
                        }
                    }

                    RunResult rr = engine.run(degree, inputs);
                    if (rr == null) {
                        System.out.println("No program loaded yet. Use option 1 first.");
                        break;
                    }

                    // show program actually executed
                    System.out.println("Executed program:");
                    for (var c : rr.executedProgram().commands()) {
                        String ln = String.format("#%d (%s) [%-5s] %s (%d)",
                                c.number(), c.basic() ? "B" : "S",
                                c.labelOrEmpty() == null ? "" : c.labelOrEmpty(),
                                c.text(), c.cycles());
                        System.out.println(ln);
                    }

                    // show results
                    System.out.println("y = " + rr.y());
                    rr.variables().forEach((k,v) -> System.out.println(k + " = " + v));
                    System.out.println("cycles = " + rr.cycles());
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
                default -> System.out.println("Invalid option.");
            }
        }
    }
}
