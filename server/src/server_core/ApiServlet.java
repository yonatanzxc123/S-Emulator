package server_core;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


@WebServlet(name = "ApiServlet", urlPatterns = {"/api/*"}, loadOnStartup = 1)
public class ApiServlet extends HttpServlet {

    // In -Memory state (no persistence)
    private static final Map<String, User> USERS = new ConcurrentHashMap<>();
    private static final AtomicLong VERSION = new AtomicLong(1); // for future polling/deltas

  //Routing requests\responses
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      String p = path(req);

      switch (p) {
          case "/health" -> json(resp, 200, "{\"ok\":true}");
          case "/users/online" -> handleUsersOnline(req, resp);
          default -> json(resp, 404, "{\"error\":\"not_found\",\"path\":\"" + esc(p) + "\"}");
      }
  }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String p = path(req);

        switch (p) {
            case "/login" -> handleLogin(req, resp);
            case "/users/credits/add" -> handleCreditsAdd(req, resp);
            case "/programs/upload" -> handleProgramUploadStub(req, resp); // stub for now
            default -> json(resp, 404, "{\"error\":\"not_found\",\"path\":\"" + esc(p) + "\"}");
        }
    }



    // Handlers
    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String body = readBody(req);
        String username = jStr(body, "username");
        if (username == null || username.isBlank()) {
            json(resp, 400, "{\"error\":\"missing_username\"}");
            return;
        }
        username = username.trim();

        // unique username across server
        if (USERS.containsKey(username)) {
            json(resp, 409, "{\"error\":\"username_taken\"}");
            return;
        }

        // create + bind to session
        User u = new User(username);
        USERS.put(username, u);
        req.getSession(true).setAttribute("username", username);
        VERSION.incrementAndGet();

        json(resp, 200, "{\"ok\":true,\"username\":\"" + esc(username) + "\",\"credits\":" + u.credits.get() + "}");
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
        // touch lastSeen for caller if logged in
        User me = optUser(req);
        if (me != null) me.lastSeenMs = System.currentTimeMillis();

        StringBuilder sb = new StringBuilder();
        sb.append("{\"users\":[");
        boolean first = true;
        for (User u : USERS.values()) {
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

    // Upload STUB: accepts JSON; no engine yet, no disk writes
    private void handleProgramUploadStub(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User u = requireUser(req, resp);
        if (u == null) return;

        String body = readBody(req);
        String name = jStr(body, "name");
        String xml  = jStr(body, "xml");   // raw XML as string
        if (name == null || name.isBlank() || xml == null || xml.isBlank()) {
            json(resp, 400, "{\"error\":\"missing_name_or_xml\"}");
            return;
        }

        // Later: hook engine + validations here.
        u.mainUploaded.incrementAndGet();
        VERSION.incrementAndGet();
        json(resp, 200, "{\"ok\":true,\"addedProgram\":{\"name\":\"" + esc(name.trim()) + "\",\"owner\":\"" + esc(u.name) + "\"}}");
    }

    //Helpers
    private static String path(HttpServletRequest req) {
        String p = req.getPathInfo();
        return (p == null || p.isEmpty()) ? "/" : p;
    }

    private static void json(HttpServletResponse resp, int code, String json) throws IOException {
        resp.setStatus(code);
        resp.setContentType("application/json; charset=UTF-8");
        try (PrintWriter out = resp.getWriter()) { out.write(json); }
    }

    private static String readBody(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = req.getReader()) {
            String line; while ((line = br.readLine()) != null) sb.append(line).append('\n');
        }
        return sb.toString();
    }

    private static String esc(String s) {
        return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n");
    }

    private static User optUser(HttpServletRequest req) {
        Object o = req.getSession(false) == null ? null : req.getSession(false).getAttribute("username");
        return (o == null) ? null : USERS.get(o.toString());
    }
    private static User requireUser(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User u = optUser(req);
        if (u == null) {
            json(resp, 401, "{\"error\":\"not_logged_in\"}");
            return null;
        }
        u.lastSeenMs = System.currentTimeMillis();
        return u;
    }

    // super-naive JSON pickers (flat JSON only)
    private static String jStr(String json, String key) {
        if (json == null) return null;
        String n = "\"" + key + "\"";
        int i = json.indexOf(n); if (i < 0) return null;
        int c = json.indexOf(':', i + n.length()); if (c < 0) return null;
        int q1 = json.indexOf('"', c + 1); if (q1 < 0) return null;
        int q2 = json.indexOf('"', q1 + 1); if (q2 < 0) return null;
        return json.substring(q1 + 1, q2);
    }
    private static Long jLong(String json, String key) {
        if (json == null) return null;
        String n = "\"" + key + "\"";
        int i = json.indexOf(n); if (i < 0) return null;
        int c = json.indexOf(':', i + n.length()); if (c < 0) return null;
        int s = c + 1;
        while (s < json.length() && Character.isWhitespace(json.charAt(s))) s++;
        int e = s;
        while (e < json.length() && "-0123456789".indexOf(json.charAt(e)) >= 0) e++;
        if (s == e) return null;
        try { return Long.parseLong(json.substring(s, e)); } catch (NumberFormatException ex) { return null; }
    }



}
