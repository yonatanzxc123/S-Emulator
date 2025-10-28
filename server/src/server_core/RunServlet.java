// java
package server_core;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import server_core.util.Credits;
import system.core.exec.FunctionEnv;

@WebServlet(name = "RunServlet", urlPatterns = {"/api/run/*"}, loadOnStartup = 1)
public class RunServlet extends BaseApiServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        switch (subPath(req)) {
            case "/start" -> handleRunStart(req, resp);
            case "/inputs" -> handleInputsRequest(req, resp);
            default -> json(resp, 404, "{\"error\":\"not_found\",\"path\":\"" + esc(subPath(req)) + "\"}");
        }
    }

    private void handleInputsRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User u = requireUser(req, resp);
        if (u == null) return;

        String body = readBody(req);
        String program = jStr(body, "program");
        String function = jStr(body, "function");
        if (function != null && !function.isBlank()) {
            FunctionMeta fmeta = FUNCTIONS.get(function);
            if (fmeta == null) {
                json(resp, 404, "{\"error\":\"function_not_found\"}");
                return;
            }
            ProgramMeta meta = PROGRAMS.get(fmeta.definedInProgram());
            if (meta == null) {
                json(resp, 404, "{\"error\":\"program_not_found\"}");
                return;
            }
            var fnMap = meta.engine.getFunctions();
            var fnBody = fnMap.get(function);
            if (fnBody == null) {
                json(resp, 404, "{\"error\":\"function_body_not_found\"}");
                return;
            }
            var view = system.core.io.ProgramMapper.toView(fnBody);
            if (view == null) {
                json(resp, 500, "{\"error\":\"function_view_unavailable\"}");
                return;
            }
            List<String> inputs = view.inputsUsed();
            StringBuilder sb = new StringBuilder("{\"ok\":true,\"inputs\":[");
            for (int i = 0; i < inputs.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("{\"name\":\"").append(esc(inputs.get(i))).append("\",\"value\":0}");
            }
            sb.append("]}");
            json(resp, 200, sb.toString());
            return;
        }

        if (program == null || program.isBlank()) {
            json(resp, 400, "{\"error\":\"bad_params\"}");
            return;
        }
        ProgramMeta meta = PROGRAMS.get(program);
        if (meta == null) {
            json(resp, 404, "{\"error\":\"program_not_found\"}");
            return;
        }

        var view = meta.engine.getProgramView();
        if (view == null) {
            json(resp, 500, "{\"error\":\"program_view_unavailable\"}");
            return;
        }

        List<String> inputs = view.inputsUsed();
        StringBuilder sb = new StringBuilder("{\"ok\":true,\"inputs\":[");
        for (int i = 0; i < inputs.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("{\"name\":\"").append(esc(inputs.get(i))).append("\",\"value\":0}");
        }
        sb.append("]}");
        json(resp, 200, sb.toString());
    }

    private void handleRunStart(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User u = requireUser(req, resp);
        if (u == null) return;

        String body    = readBody(req);
        String program = jStr(body, "program");
        String arch    = jStr(body, "arch");
        Long   degreeL = jLong(body, "degree");
        int    degree  = (degreeL == null ? 0 : degreeL.intValue());
        String function = jStr(body, "function");

        if ((program == null || program.isBlank()) || !Credits.validArch(arch)) {
            json(resp, 400, "{\"error\":\"bad_params\"}");
            return;
        }
        ProgramMeta meta = PROGRAMS.get(program);
        if (meta == null) {
            json(resp, 404, "{\"error\":\"program_not_found\"}");
            return;
        }

        long requiredMin = Credits.minRequiredToStart(meta, arch);
        if (u.getCredits() < requiredMin) {
            json(resp, 402, "{\"error\":\"insufficient_credits\",\"required\":" + requiredMin + "}");
            return;
        }

        long archFixed = Credits.archFixed(arch);
        if (!Credits.tryCharge(u, archFixed)) {
            json(resp, 402, "{\"error\":\"insufficient_credits\"}");
            return;
        }

        List<Long> inputs = jLongList(body, "inputs");
        system.api.RunResult rr;
        try {
            System.out.println("RunServlet: function=" + function);
            if (function != null && !function.isBlank()) {
                FunctionMeta fmeta = FUNCTIONS.get(function);
                if (fmeta == null) {
                    u.addCredits(archFixed);
                    json(resp, 404, "{\"error\":\"function_not_found\"}");
                    return;
                }
                var fnMap = meta.engine.getFunctions();
                var fnBody = fnMap.get(function);
                if (fnBody == null) {
                    u.addCredits(archFixed);
                    json(resp, 404, "{\"error\":\"function_body_not_found\"}");
                    return;
                }
                rr = FunctionEnv.with(new FunctionEnv(fnMap), () -> {
                    system.core.model.Program toRun = (degree == 0) ? fnBody
                            : new system.core.expand.ExpanderImpl().expandToDegree(fnBody, degree);

                    var exec = new system.core.exec.Executor();
                    var st = exec.run(toRun, inputs);

                    var vars = new java.util.LinkedHashMap<String, Long>();
                    vars.put("y", st.y());
                    var xs = new java.util.TreeMap<>(st.snapshotX());
                    var zs = new java.util.TreeMap<>(st.snapshotZ());
                    xs.forEach((i, v) -> vars.put("x" + i, v));
                    zs.forEach((i, v) -> vars.put("z" + i, v));

                    return new system.api.RunResult(st.y(), st.cycles(), null, vars);
                });
            } else {
                rr = meta.engine.run(degree, inputs);
            }
        } catch (Exception e) {
            u.addCredits(archFixed);
            json(resp, 500, "{\"error\":\"engine_run_error\"}");
            return;
        }

        long cycles = rr.cycles();
        long y      = rr.y();

        if (!Credits.tryCharge(u, cycles)) {
            long remaining = u.getCredits();
            if (remaining > 0) {
                Credits.tryCharge(u, remaining);
            }
            u.creditsSpent.addAndGet(archFixed + remaining);
            json(resp, 409, "{\"error\":\"credit_exhausted\",\"charged\":" + (archFixed + remaining) + "}");
            return;
        } else {
            u.creditsSpent.addAndGet(archFixed + cycles);
        }

        u.runsCount.incrementAndGet();

        boolean isMainProgram = jBool(body, "isMainProgram", function == null || function.isBlank());

        u.addRunRecord(new User.RunRecord(
                u.runsCount.get(),
                isMainProgram,
                (function != null && !function.isBlank()) ? function : program,
                arch,
                degree,
                rr.y(),
                rr.cycles(),
                inputs,
                rr.variablesOrdered()
        ));
        long totalUsed = archFixed + cycles;
        long newRunCount = meta.runsCount.incrementAndGet();
        meta.avgCreditsCost = ((meta.avgCreditsCost * (newRunCount - 1)) + totalUsed) / (double) newRunCount;

        var vars = rr.variablesOrdered();

        StringBuilder sb = new StringBuilder("{");
        sb.append("\"ok\":true,");
        sb.append("\"program\":\"").append(esc(program)).append("\",");
        sb.append("\"arch\":\"").append(esc(arch)).append("\",");
        sb.append("\"degree\":").append(degree).append(",");
        sb.append("\"cycles\":").append(cycles).append(",");
        sb.append("\"y\":").append(y).append(",");
        sb.append("\"creditsLeft\":").append(u.getCredits()).append(",");
        sb.append("\"vars\":{");
        boolean first = true;
        for (var e : vars.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(esc(e.getKey())).append("\":").append(e.getValue());
        }
        sb.append("}}");
        json(resp, 200, sb.toString());
    }
}
