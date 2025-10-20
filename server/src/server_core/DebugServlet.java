package server_core;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import system.api.DebugStep;
import system.core.exec.debugg.Debugger;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import server_core.util.Credits;
import server_core.util.DebugSession;

@WebServlet(name = "DebugServlet", urlPatterns = {"/api/debug/*"}, loadOnStartup = 1)
public class DebugServlet extends BaseApiServlet {

    // In-memory debug sessions (live only for this JVM uptime)
    private static final Map<String, DebugSession> SESSIONS = new ConcurrentHashMap<>();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String sp = subPath(req);
        switch (sp) {
            case "/start"       -> start(req, resp);
            case "/step"        -> step(req, resp);
            case "/resume"      -> resume(req, resp);
            case "/back"        -> back(req, resp);
            case "/breakpoints" -> breakpoints(req, resp);
            case "/stop"        -> stop(req, resp);
            default             -> json(resp, 404, "{\"error\":\"not_found\",\"path\":\"" + esc(sp) + "\"}");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if ("/state".equals(subPath(req))) {
            state(req, resp);
        } else {
            json(resp, 404, "{\"error\":\"not_found\",\"path\":\"" + esc(subPath(req)) + "\"}");
        }
    }

    // ---------- POST /api/debug/start ----------
    private void start(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User u = requireUser(req, resp);
        if (u == null) return;

        String body    = readBody(req);
        String program = jStr(body, "program");
        Long   degL    = jLong(body, "degree");
        int    degree  = (degL == null ? 0 : degL.intValue());
        String arch    = jStr(body, "arch");

        if (program == null || program.isBlank() || !Credits.validArch(arch)) {
            json(resp, 400, "{\"error\":\"bad_params\"}");
            return;
        }
        ProgramMeta meta = PROGRAMS.get(program);
        if (meta == null) {
            json(resp, 404, "{\"error\":\"program_not_found\"}");
            return;
        }

        long required = Credits.minRequiredToStart(meta, arch);
        if (u.getCredits() < required) {
            json(resp, 402, "{\"error\":\"insufficient_credits\",\"required\":" + required + "}");
            return;
        }

        // Pre-charge the fixed architecture cost for starting debug
        long fixed = Credits.archFixed(arch);
        if (!Credits.tryCharge(u, fixed)) {
            json(resp, 402, "{\"error\":\"insufficient_credits\"}");
            return;
        }

        Debugger dbg;
        try {
            dbg = meta.engine.startDebug(degree, parseInputs(body));
            if (dbg == null) throw new IllegalStateException("Debugger initialization failed");
        } catch (Exception ex) {
            // Roll back fixed cost on failure to start debugger
            u.addCredits(fixed);
            json(resp, 500, "{\"error\":\"debug_start_failed\"}");
            return;
        }

        // Create a new debug session with initial state
        DebugStep snap = dbg.peek();
        String sessionId = UUID.randomUUID().toString();
        SESSIONS.put(sessionId, new DebugSession(
                sessionId, program, arch, degree,
                dbg, fixed, 0L, snap.cycles()
        ));

        json(resp, 200, "{"
                + "\"ok\":true,"
                + "\"id\":\"" + sessionId + "\","
                + "\"creditsLeft\":" + u.getCredits()
                + "}");
    }

    // ---------- POST /api/debug/step ----------
    private void step(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doAdvance(req, resp, Mode.STEP);
    }

    // ---------- POST /api/debug/resume ----------
    private void resume(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doAdvance(req, resp, Mode.RESUME);
    }

    // Mode for advancing the debugger
    private enum Mode { STEP, RESUME, BACK }

    /** Handle stepping, resuming, or stepping back in a debug session. */
    private void doAdvance(HttpServletRequest req, HttpServletResponse resp, Mode mode) throws IOException {
        User u = requireUser(req, resp);
        if (u == null) return;

        String body = readBody(req);
        String id   = jStr(body, "id");
        if (id == null || id.isBlank()) {
            json(resp, 400, "{\"error\":\"missing_id\"}");
            return;
        }
        DebugSession s = SESSIONS.get(id);
        if (s == null) {
            json(resp, 404, "{\"error\":\"session_not_found\"}");
            return;
        }

        // Batch size for step mode (defaults to 1)
        long batchSteps = Math.max(1, Optional.ofNullable(jLong(body, "n")).orElse(1L));
        DebugStep snap;
        try {
            switch (mode) {
                case STEP -> {
                    snap = s.dbg.peek();
                    long steps = batchSteps;
                    while (steps-- > 0 && !snap.finished()) {
                        snap = s.dbg.step();  // execute one instruction
                        long delta = Math.max(0, snap.cycles() - s.lastCycles);
                        if (delta > 0 && !Credits.tryCharge(u, delta)) {
                            // Out of credits during this step - revert the last step
                            try {
                                snap = s.dbg.stepBack();
                            } catch (Exception ex) {
                                // If stepping back fails, the session may have advanced without payment
                            }
                            // No charge was applied (credits rollback happened), so user still has same credits
                            json(resp, 409, exhaustedJson(s, delta, u.getCredits()));
                            return;
                        }
                        // Accumulate charged cycles and update baseline
                        s.chargedCycles += delta;
                        s.lastCycles = snap.cycles();
                    }
                }
                case RESUME -> {
                    // Resume execution until a breakpoint or program halt
                    snap = s.dbg.resume();
                    long delta = Math.max(0, snap.cycles() - s.lastCycles);
                    if (delta > 0 && !Credits.tryCharge(u, delta)) {
                        // Out of credits during resume - attempt to roll back all steps taken during resume
                        long initialCycles = s.lastCycles;
                        try {
                            while (snap.cycles() > initialCycles) {
                                snap = s.dbg.stepBack();
                            }
                        } catch (Exception ex) {
                            // If unable to fully revert, session state may have advanced beyond paid cycles
                        }
                        json(resp, 409, exhaustedJson(s, delta, u.getCredits()));
                        return;
                    }
                    s.chargedCycles += delta;
                    s.lastCycles = snap.cycles();
                }
                case BACK -> {
                    // Step backwards (no credit charge for stepping back)
                    snap = s.dbg.stepBack();
                    // Update lastCycles baseline to the new (decremented) cycles count
                    s.lastCycles = snap.cycles();
                }
                default -> throw new IllegalStateException("Unexpected mode: " + mode);
            }
        } catch (Exception ex) {
            String errCode = switch (mode) {
                case STEP   -> "debug_step_failed";
                case RESUME -> "debug_resume_failed";
                case BACK   -> "debug_back_failed";
            };
            json(resp, 500, "{\"error\":\"" + errCode + "\"}");
            return;
        }

        // On success, return the current debug state snapshot
        json(resp, 200, stateJson(s, snap, u.getCredits()));
    }

    // ---------- POST /api/debug/back ----------
    private void back(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doAdvance(req, resp, Mode.BACK);
    }

    // ---------- POST /api/debug/breakpoints ----------
    private void breakpoints(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User u = requireUser(req, resp);
        if (u == null) return;

        String body = readBody(req);
        String id   = jStr(body, "id");
        if (id == null || id.isBlank()) {
            json(resp, 400, "{\"error\":\"missing_id\"}");
            return;
        }
        DebugSession s = SESSIONS.get(id);
        if (s == null) {
            json(resp, 404, "{\"error\":\"session_not_found\"}");
            return;
        }

        List<Integer> pcs = parseIntArray(body, "pcs");
        try {
            s.dbg.setBreakpoints(pcs);
        } catch (Exception ex) {
            json(resp, 500, "{\"error\":\"breakpoints_set_failed\"}");
            return;
        }
        json(resp, 200, "{\"ok\":true,\"pcs\":" + pcs.toString() + "}");
    }

    // ---------- GET /api/debug/state ----------
    private void state(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User u = requireUser(req, resp);
        if (u == null) return;

        String id = req.getParameter("id");
        if (id == null || id.isBlank()) {
            json(resp, 400, "{\"error\":\"missing_id\"}");
            return;
        }
        DebugSession s = SESSIONS.get(id);
        if (s == null) {
            json(resp, 404, "{\"error\":\"session_not_found\"}");
            return;
        }

        DebugStep snap = s.dbg.peek();
        json(resp, 200, stateJson(s, snap, u.getCredits()));
    }

    // ---------- POST /api/debug/stop ----------
    private void stop(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User u = requireUser(req, resp);
        if (u == null) return;

        String body = readBody(req);
        String id   = jStr(body, "id");
        if (id == null || id.isBlank()) {
            json(resp, 400, "{\"error\":\"missing_id\"}");
            return;
        }
        DebugSession s = SESSIONS.remove(id);
        if (s == null) {
            json(resp, 404, "{\"error\":\"session_not_found\"}");
            return;
        }

        // When stopping, record all credits spent during this session (fixed + variable)
        u.creditsSpent.addAndGet(s.fixed + s.chargedCycles);
        json(resp, 200, "{\"ok\":true}");
    }

    // ---------- Helper methods for parsing inputs and building JSON ----------
    private static List<Long> parseInputs(String json) {
        int i = (json == null ? -1 : json.indexOf("\"inputs\""));
        if (i < 0) return List.of();
        int c = json.indexOf(':', i + 8); if (c < 0) return List.of();
        int s = json.indexOf('[', c + 1); if (s < 0) return List.of();
        int e = json.indexOf(']', s + 1); if (e < 0) return List.of();
        if (e <= s + 1) return List.of();
        String[] toks = json.substring(s + 1, e).split(",");
        List<Long> out = new ArrayList<>(toks.length);
        for (String t : toks) {
            try {
                out.add(Long.parseLong(t.trim()));
            } catch (NumberFormatException ignore) {}
        }
        return out;
    }

    private static List<Integer> parseIntArray(String json, String key) {
        int i = (json == null ? -1 : json.indexOf("\"" + key + "\""));
        if (i < 0) return List.of();
        int c = json.indexOf(':', i + key.length() + 2); if (c < 0) return List.of();
        int s = json.indexOf('[', c + 1); if (s < 0) return List.of();
        int e = json.indexOf(']', s + 1); if (e < 0) return List.of();
        if (e <= s + 1) return List.of();
        String[] toks = json.substring(s + 1, e).split(",");
        List<Integer> out = new ArrayList<>(toks.length);
        for (String t : toks) {
            try {
                out.add(Integer.parseInt(t.trim()));
            } catch (NumberFormatException ignore) {}
        }
        return out;
    }

    /** Construct a JSON error response for credit exhaustion during debug steps/resume. */
    private static String exhaustedJson(DebugSession s, long delta, long creditsLeft) {
        return "{"
                + "\"error\":\"credit_exhausted\","
                + "\"chargedThisStep\":" + delta + ","
                + "\"chargedTotal\":" + (s.fixed + s.chargedCycles) + ","
                + "\"creditsLeft\":" + creditsLeft
                + "}";
    }

    /** Construct a JSON string for the current debugger state (including variables and credits info). */
    private static String stateJson(DebugSession s, DebugStep snap, long creditsLeft) {
        // The variables map contains all registers (y, x's, z's) at the current state
        Map<String, Long> vars = snap.vars();

        StringBuilder sb = new StringBuilder(256);
        sb.append("{\"ok\":true,")
                .append("\"id\":\"").append(esc(s.id)).append("\",")
                .append("\"program\":\"").append(esc(s.program)).append("\",")
                .append("\"arch\":\"").append(esc(s.arch)).append("\",")
                .append("\"degree\":").append(s.degree).append(',')
                .append("\"halted\":").append(snap.finished()).append(',')
                .append("\"pc\":").append(snap.pc()).append(',')
                .append("\"cycles\":").append(snap.cycles()).append(',')
                .append("\"chargedTotal\":").append(s.fixed + s.chargedCycles).append(',')
                .append("\"creditsLeft\":").append(creditsLeft).append(',')
                .append("\"vars\":{");

        boolean firstVar = true;
        for (var entry : vars.entrySet()) {
            if (!firstVar) sb.append(',');
            firstVar = false;
            sb.append('"').append(esc(entry.getKey())).append("\":").append(entry.getValue());
        }
        sb.append("}}");
        return sb.toString();
    }
}
