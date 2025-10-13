// java
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

    // Look up the logged-in user from the session.
    // Prefer the "user" attribute (set by AuthServlet), with a legacy fallback to "username".
    protected static User optUser(HttpServletRequest req) {
        var ses = req.getSession(false);
        if (ses == null) return null;

        Object uAttr = ses.getAttribute("user");
        if (uAttr instanceof User u) {
            return u;
        }

        // Backward-compatible: if only username is present, materialize a User and cache it
        Object nameAttr = ses.getAttribute("username");
        if (nameAttr != null) {
            String name = String.valueOf(nameAttr);
            User u = USERS.computeIfAbsent(name, User::new);
            ses.setAttribute("user", u); // migrate to the canonical attribute
            return u;
        }

        return null;
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
        String k = "\"" + key + "\"";
        int i = json.indexOf(k);
        if (i < 0) return null;
        int c = json.indexOf(':', i + k.length());
        if (c < 0) return null;

        int p = c + 1, n = json.length();
        while (p < n && Character.isWhitespace(json.charAt(p))) p++;
        if (p >= n || json.charAt(p) != '"') return null;

        StringBuilder out = new StringBuilder(json.length() - p);
        boolean esc = false;
        for (int q = p + 1; q < n; q++) {
            char ch = json.charAt(q);
            if (esc) {
                switch (ch) {
                    case '"': out.append('"'); break;
                    case '\\': out.append('\\'); break;
                    case '/': out.append('/'); break;
                    case 'b': out.append('\b'); break;
                    case 'f': out.append('\f'); break;
                    case 'n': out.append('\n'); break;
                    case 'r': out.append('\r'); break;
                    case 't': out.append('\t'); break;
                    case 'u':
                        if (q + 4 < n) {
                            int h = (hex(json.charAt(q + 1)) << 12)
                                    | (hex(json.charAt(q + 2)) << 8)
                                    | (hex(json.charAt(q + 3)) << 4)
                                    | (hex(json.charAt(q + 4)));
                            out.append((char) h);
                            q += 4;
                        }
                        break;
                    default:
                        out.append(ch);
                        break;
                }
                esc = false;
            } else if (ch == '\\') {
                esc = true;
            } else if (ch == '"') {
                return out.toString();
            } else {
                out.append(ch);
            }
        }
        return null;
    }

    private static int hex(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'f') return 10 + (c - 'a');
        if (c >= 'A' && c <= 'F') return 10 + (c - 'A');
        return 0;
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
