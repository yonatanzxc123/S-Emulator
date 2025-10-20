package server_core;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import server_core.util.Credits;

@WebServlet(name = "RunServlet", urlPatterns = {"/api/run/*"}, loadOnStartup = 1)
public class RunServlet extends BaseApiServlet {

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

        String body    = readBody(req);
        String program = jStr(body, "program");
        String arch    = jStr(body, "arch");
        Long   degreeL = jLong(body, "degree");
        int    degree  = (degreeL == null ? 0 : degreeL.intValue());

        // Validate parameters
        if (program == null || program.isBlank() || !Credits.validArch(arch)) {
            json(resp, 400, "{\"error\":\"bad_params\"}");
            return;
        }
        ProgramMeta meta = PROGRAMS.get(program);
        if (meta == null) {
            json(resp, 404, "{\"error\":\"program_not_found\"}");
            return;
        }

        // Ensure user has at least the fixed + average cost to start
        long requiredMin = Credits.minRequiredToStart(meta, arch);
        if (u.getCredits() < requiredMin) {
            json(resp, 402, "{\"error\":\"insufficient_credits\",\"required\":" + requiredMin + "}");
            return;
        }

        // Charge fixed architecture cost up front
        long archFixed = Credits.archFixed(arch);
        if (!Credits.tryCharge(u, archFixed)) {
            // (Should not happen if the above check passed, but just in case)
            json(resp, 402, "{\"error\":\"insufficient_credits\"}");
            return;
        }

        // Execute the program run
        system.api.RunResult rr;
        try {
            rr = meta.engine.run(degree, List.of());  // supply inputs if needed
        } catch (Exception e) {
            // Roll back the fixed cost if the engine run fails
            u.addCredits(archFixed);
            json(resp, 500, "{\"error\":\"engine_run_error\"}");
            return;
        }

        long cycles = rr.cycles();
        long y      = rr.y();

        // Attempt to charge for the variable cost (cycles); handle insufficient credits
        if (!Credits.tryCharge(u, cycles)) {
            long remaining = u.getCredits();  // credits still available (tryCharge rolled back if insufficient)
            if (remaining > 0) {
                // Deduct all remaining credits as a partial payment
                Credits.tryCharge(u, remaining);
            }
            // Record the credits that were actually spent (fixed + remaining) and return error
            u.creditsSpent.addAndGet(archFixed + remaining);
            json(resp, 409, "{\"error\":\"credit_exhausted\",\"charged\":" + (archFixed + remaining) + "}");
            return;
        } else {
            // Full charge succeeded, record total credits spent
            u.creditsSpent.addAndGet(archFixed + cycles);
        }

        // Update usage statistics
        u.runsCount.incrementAndGet();
        long totalUsed = archFixed + cycles;
        long newRunCount = meta.runsCount.incrementAndGet();
        // Update average credits cost (note: not atomic, but minor race conditions are acceptable here)
        meta.avgCreditsCost = ((meta.avgCreditsCost * (newRunCount - 1)) + totalUsed) / (double) newRunCount;

        // Respond with success and run results
        json(resp, 200, "{"
                + "\"ok\":true,"
                + "\"program\":\"" + esc(program) + "\","
                + "\"arch\":\"" + esc(arch) + "\","
                + "\"degree\":" + degree + ","
                + "\"cycles\":" + cycles + ","
                + "\"y\":" + y + ","
                + "\"creditsLeft\":" + u.getCredits()
                + "}");
    }
}
