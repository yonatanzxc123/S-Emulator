package server_core;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "UsersServlet", urlPatterns = {"/api/users/*"}, loadOnStartup = 1)
public class UsersServlet extends BaseApiServlet {

    private static final long ACTIVE_TTL_MS = 4000L;  // user considered "online" if seen within last 4 seconds

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        switch (subPath(req)) {
            case "/online" -> handleUsersOnline(req, resp);
            case "/history" -> handleUserHistory(req, resp);
            default        -> json(resp, 404, "{\"error\":\"not_found\",\"path\":\"" + esc(subPath(req)) + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        switch (subPath(req)) {
            case "/credits/add" -> handleCreditsAdd(req, resp);
            default             -> json(resp, 404, "{\"error\":\"not_found\",\"path\":\"" + esc(subPath(req)) + "\"}");
        }
    }

    private void handleUserHistory(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User u = requireUser(req, resp);
        if (u == null) return;
        var history = u.getRunHistory();
        StringBuilder sb = new StringBuilder("{\"ok\":true,\"history\":[");
        boolean first = true;
        for (User.RunRecord r : history) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{")
                    .append("\"runNo\":").append(r.runNo).append(",")
                    .append("\"isMainProgram\":").append(r.isMainProgram).append(",")
                    .append("\"name\":\"").append(esc(r.name)).append("\",")
                    .append("\"arch\":\"").append(esc(r.arch)).append("\",")
                    .append("\"degree\":").append(r.degree).append(",")
                    .append("\"y\":").append(r.y).append(",")
                    .append("\"cycles\":").append(r.cycles).append(",")
                    .append("\"inputs\":").append(r.inputs == null ? "[]" : r.inputs.toString()).append(",")
                    .append("\"vars\":{");
            boolean firstVar = true;
            for (var e : r.vars.entrySet()) {
                if (!firstVar) sb.append(",");
                firstVar = false;
                sb.append("\"").append(esc(e.getKey())).append("\":").append(e.getValue());
            }
            sb.append("}");
            sb.append("}");
        }
        sb.append("]}");
        System.out.println("User history JSON: " + sb);
        json(resp, 200, sb.toString());
    }

    private void handleCreditsAdd(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User u = requireUser(req, resp);
        if (u == null) return;

        String body = readBody(req);
        Long amount = jLong(body, "amount");
        if (amount == null || amount <= 0) {
            json(resp, 400, "{\"error\":\"invalid_amount\"}");
            return;
        }
        long newBalance = u.addCredits(amount);
        VERSION.incrementAndGet();
        json(resp, 200, "{\"ok\":true,\"credits\":" + newBalance + "}");
    }

    private void handleUsersOnline(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long now = System.currentTimeMillis();
        User me = optUser(req);
        if (me != null) {
            me.lastSeenMs = now;
        }

        StringBuilder sb = new StringBuilder("{\"users\":[");
        boolean first = true;
        for (User user : USERS.values()) {
            if (now - user.lastSeenMs > ACTIVE_TTL_MS) continue;  // only include recently active users
            if (!first) sb.append(',');
            first = false;
            sb.append("{\"name\":\"").append(esc(user.name)).append("\",")
                    .append("\"mainUploaded\":").append(user.mainUploaded.get()).append(',')
                    .append("\"helperContrib\":").append(user.helperContrib.get()).append(',')
                    .append("\"credits\":").append(user.getCredits()).append(',')
                    .append("\"creditsSpent\":").append(user.creditsSpent.get()).append(',')
                    .append("\"runsCount\":").append(user.runsCount.get()).append('}');
        }
        sb.append("]}");
        json(resp, 200, sb.toString());
    }
}
