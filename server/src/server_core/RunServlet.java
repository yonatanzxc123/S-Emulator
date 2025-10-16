package server_core;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet(name = "RunServlet", urlPatterns = {"/api/run/*"}, loadOnStartup = 1)
public class RunServlet extends BaseApiServlet {

    private static final Map<String,Integer> ARCH_COST = Map.of(
            "I", 5, "II", 100, "III", 500, "IV", 1000
    );

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        switch (subPath(req)) {
            case "/start" -> handleRunStart(req, resp);
            default -> json(resp, 404, "{\"error\":\"not_found\",\"path\":\"" + esc(subPath(req)) + "\"}");
        }
    }

    private void handleRunStart(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User u = requireUser(req, resp);
        if (u == null) return;

        String body = readBody(req);
        String program = jStr(body, "program");
        String arch = jStr(body, "arch");
        Long degreeL = jLong(body, "degree");
        int degree = degreeL == null ? 0 : degreeL.intValue();

        if (program == null || program.isBlank() || arch == null || !ARCH_COST.containsKey(arch)) {
            json(resp, 400, "{\"error\":\"bad_params\"}");
            return;
        }
        ProgramMeta meta = PROGRAMS.get(program);
        if (meta == null) { json(resp, 404, "{\"error\":\"program_not_found\"}"); return; }

        long archFixed = ARCH_COST.get(arch);
        long avgCredits = Math.round(meta.avgCreditsCost);
        long requiredMin = archFixed + Math.max(0, avgCredits);
        if (u.credits.get() < requiredMin) {
            json(resp, 402, "{\"error\":\"insufficient_credits\",\"required\":" + requiredMin + "}");
            return;
        }

        if (u.credits.addAndGet(-archFixed) < 0) {
            u.credits.addAndGet(archFixed);
            json(resp, 402, "{\"error\":\"insufficient_credits\"}");
            return;
        }

        system.api.RunResult rr;
        try {
            rr = meta.engine.run(degree, List.of());  // supply inputs if needed
        } catch (Exception e) {
            u.credits.addAndGet(archFixed);
            json(resp, 500, "{\"error\":\"engine_run_error\"}");
            return;
        }

        long cycles = rr.cycles();
        long y      = rr.y();

        long available = u.credits.get();
        if (available < cycles) {
            u.credits.addAndGet(-available);
            u.creditsSpent.addAndGet(archFixed + available);
            json(resp, 409, "{\"error\":\"credit_exhausted\",\"charged\":" + (archFixed + available) + "}");
            return;
        } else {
            u.credits.addAndGet(-cycles);
            u.creditsSpent.addAndGet(archFixed + cycles);
        }

        u.runsCount.incrementAndGet();
        long totalCredits = archFixed + cycles;
        long newRuns = meta.runsCount.incrementAndGet();
        meta.avgCreditsCost = ((meta.avgCreditsCost * (newRuns - 1)) + totalCredits) / (double) newRuns;

        json(resp, 200, "{"
                + "\"ok\":true,"
                + "\"program\":\"" + esc(program) + "\","
                + "\"arch\":\"" + esc(arch) + "\","
                + "\"degree\":" + degree + ","
                + "\"cycles\":" + cycles + ","
                + "\"y\":" + y + ","
                + "\"creditsLeft\":" + u.credits.get()
                + "}");
    }
}
