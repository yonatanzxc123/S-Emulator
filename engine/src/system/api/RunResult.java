package system.api;

import system.api.view.ProgramView;
import java.util.*;

public record RunResult(long y, long cycles, ProgramView executedProgram, Map<String,Long> variables) {

    /** Spec order: y, then x1..xn, then z1..zm  as Aviad wanted or god ? */
    public Map<String,Long> variablesOrdered() {
        LinkedHashMap<String,Long> out = new LinkedHashMap<>();
        out.put("y", y);

        List<Map.Entry<String,Long>> xs = new ArrayList<>();
        List<Map.Entry<String,Long>> zs = new ArrayList<>();

        for (var e : variables.entrySet()) {
            String k = e.getKey();
            if ("y".equals(k)) continue;
            if (k.startsWith("x")) xs.add(e);
            else if (k.startsWith("z")) zs.add(e);
        }
        xs.sort(Comparator.comparingInt(e -> Integer.parseInt(e.getKey().substring(1))));
        zs.sort(Comparator.comparingInt(e -> Integer.parseInt(e.getKey().substring(1))));

        xs.forEach(e -> out.put(e.getKey(), e.getValue()));
        zs.forEach(e -> out.put(e.getKey(), e.getValue()));
        return out;
    }
}
