// BaseApiServlet.java
package server_core;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

abstract class BaseApiServlet extends HttpServlet {
    // Shared in-memory state
    protected static final Map<String, User> USERS = new ConcurrentHashMap<>();
    protected static final AtomicLong VERSION = new AtomicLong(1);
    protected static final java.util.Map<String, ProgramMeta> PROGRAMS = new java.util.concurrent.ConcurrentHashMap<>();
    protected static final java.util.Map<String, FunctionMeta> FUNCTIONS = new java.util.concurrent.ConcurrentHashMap<>();

    // -------- Helpers --------
    protected static String subPath(HttpServletRequest req) {
        String p = req.getPathInfo();
        return (p == null || p.isEmpty()) ? "/" : p;
    }

    protected static void json(HttpServletResponse resp, int code, String body) throws IOException {
        resp.setStatus(code);
        resp.setContentType("application/json; charset=UTF-8");
        try (PrintWriter out = resp.getWriter()) { out.write(body); }
    }

    protected static String readBody(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = req.getReader()) {
            String line; while ((line = br.readLine()) != null) sb.append(line).append('\n');
        }
        return sb.toString();
    }

    protected static String esc(String s) {
        return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n");
    }

    protected static User optUser(HttpServletRequest req) {
        var ses = req.getSession(false);
        Object o = (ses == null) ? null : ses.getAttribute("username");
        return (o == null) ? null : USERS.get(o.toString());
    }

    protected static User requireUser(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User u = optUser(req);
        if (u == null) {
            json(resp, 401, "{\"error\":\"not_logged_in\"}");
            return null;
        }
        u.lastSeenMs = System.currentTimeMillis();
        return u;
    }

    // Naive JSON pickers (flat JSON only)
    protected static String jStr(String json, String key) {
        if (json == null) return null;
        String n = "\"" + key + "\"";
        int i = json.indexOf(n); if (i < 0) return null;
        int c = json.indexOf(':', i + n.length()); if (c < 0) return null;
        int q1 = json.indexOf('"', c + 1); if (q1 < 0) return null;
        int q2 = json.indexOf('"', q1 + 1); if (q2 < 0) return null;
        return json.substring(q1 + 1, q2);
    }
    protected static Long jLong(String json, String key) {
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

    protected static String toJsonArray(java.util.Collection<String> items) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (String s : items) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(esc(s)).append('"');
        }
        sb.append(']');
        return sb.toString();
    }


}
