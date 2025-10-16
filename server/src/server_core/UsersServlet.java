// java
package server_core;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(name = "UsersServlet", urlPatterns = {"/api/users/*"}, loadOnStartup = 1)
public class UsersServlet extends BaseApiServlet {

    private static final long ACTIVE_TTL_MS = 4000L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        switch (subPath(req)) {
            case "/online" -> handleUsersOnline(req, resp);
            default -> json(resp, 404, "{\"error\":\"not_found\",\"path\":\"" + esc(subPath(req)) + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        switch (subPath(req)) {
            case "/credits/add" -> handleCreditsAdd(req, resp);
            default -> json(resp, 404, "{\"error\":\"not_found\",\"path\":\"" + esc(subPath(req)) + "\"}");
        }
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
        long newBal = u.credits.addAndGet(amount);
        VERSION.incrementAndGet();
        json(resp, 200, "{\"ok\":true,\"credits\":" + newBal + "}");
    }

    private void handleUsersOnline(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long now = System.currentTimeMillis();
        User me = optUser(req);
        if (me != null) me.lastSeenMs = now;

        StringBuilder sb = new StringBuilder();
        sb.append("{\"users\":[");
        boolean first = true;
        for (User u : USERS.values()) {
            if (now - u.lastSeenMs > ACTIVE_TTL_MS) continue; // only active users
            if (!first) sb.append(',');
            first = false;
            sb.append("{\"name\":\"").append(esc(u.name)).append("\",")
                    .append("\"mainUploaded\":").append(u.mainUploaded.get()).append(',')
                    .append("\"helperContrib\":").append(u.helperContrib.get()).append(',')
                    .append("\"credits\":").append(u.credits.get()).append(',')
                    .append("\"creditsSpent\":").append(u.creditsSpent.get()).append(',')
                    .append("\"runsCount\":").append(u.runsCount.get()).append('}');
        }
        sb.append("]}");
        json(resp, 200, sb.toString());
    }
}
