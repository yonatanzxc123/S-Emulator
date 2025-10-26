package server_core;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import system.core.model.Program;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

abstract class BaseApiServlet extends HttpServlet {
    // Shared in-memory state (global maps and versioning)
    protected static final Map<String, User> USERS = new ConcurrentHashMap<>();
    protected static final AtomicLong VERSION = new AtomicLong(1);
    protected static final Map<String, ProgramMeta> PROGRAMS = new ConcurrentHashMap<>();
    protected static final Map<String, FunctionMeta> FUNCTIONS = new ConcurrentHashMap<>();
    protected static final Map<String, Program> FUNCTION_BODIES = new ConcurrentHashMap<>();

    // -------- Helper Methods --------

    /** Get the sub-path of the request (after the servlet mapping). */
    protected static String subPath(HttpServletRequest req) {
        String p = req.getPathInfo();
        return (p == null || p.isEmpty()) ? "/" : p;
    }

    /** Write a JSON response with the given HTTP status code. */
    protected static void json(HttpServletResponse resp, int code, String body) throws IOException {
        resp.setStatus(code);
        resp.setContentType("application/json; charset=UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            out.write(body);
        }
    }

    /** Read the entire request body as a string. */
    protected static String readBody(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = req.getReader()) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    /** Escape a string for safe inclusion in JSON (handles quotes, backslashes, newlines). */
    protected static String esc(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }

    /**
     * Retrieve the logged-in user from the session.
     * Checks the "user" attribute first (set by AuthServlet), then falls back to a legacy "username" attribute.
     */
    protected static User optUser(HttpServletRequest req) {
        var ses = req.getSession(false);
        if (ses == null) return null;

        Object uAttr = ses.getAttribute("user");
        if (uAttr instanceof User u) {
            return u;
        }
        // Backward-compatible: if only a username is stored, create or get the User object
        Object nameAttr = ses.getAttribute("username");
        if (nameAttr != null) {
            String name = String.valueOf(nameAttr);
            User u = USERS.computeIfAbsent(name, User::new);
            ses.setAttribute("user", u); // migrate to the canonical attribute
            return u;
        }
        return null;
    }

    /**
     * Require that a user is logged in for this request. If not, send a 401 and return null.
     * Updates the user's last-seen timestamp on success.
     */
    protected static User requireUser(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User u = optUser(req);
        if (u == null) {
            json(resp, 401, "{\"error\":\"not_logged_in\"}");
            return null;
        }
        u.lastSeenMs = System.currentTimeMillis();
        return u;
    }

    // ---- Basic JSON parsing helpers (for simple flat JSON structures) ----

    protected static String jStr(String json, String key) {
        if (json == null) return null;
        String token = "\"" + key + "\"";
        int i = json.indexOf(token);
        if (i < 0) return null;
        int c = json.indexOf(':', i + token.length());
        if (c < 0) return null;
        int p = c + 1, n = json.length();
        // Skip whitespace and ensure next char is a quote
        while (p < n && Character.isWhitespace(json.charAt(p))) p++;
        if (p >= n || json.charAt(p) != '"') return null;
        StringBuilder out = new StringBuilder();
        boolean escape = false;
        for (int q = p + 1; q < n; q++) {
            char ch = json.charAt(q);
            if (escape) {
                switch (ch) {
                    case '"'  -> out.append('"');
                    case '\\' -> out.append('\\');
                    case '/'  -> out.append('/');
                    case 'b'  -> out.append('\b');
                    case 'f'  -> out.append('\f');
                    case 'n'  -> out.append('\n');
                    case 'r'  -> out.append('\r');
                    case 't'  -> out.append('\t');
                    case 'u'-> {
                        if (q + 4 < n) {
                            int code = (hex(json.charAt(q+1)) << 12)
                                    | (hex(json.charAt(q+2)) << 8)
                                    | (hex(json.charAt(q+3)) << 4)
                                    | (hex(json.charAt(q+4)));
                            out.append((char) code);
                            q += 4;
                        }
                    }
                    default ->
                        out.append(ch);


                }
                escape = false;
            } else if (ch == '\\') {
                escape = true;
            } else if (ch == '"') {
                // end of string
                return out.toString();
            } else {
                out.append(ch);
            }
        }
        return null; // no closing quote found
    }

    private static int hex(char c) {
        return switch (c) {
            case '0','1','2','3','4','5','6','7','8','9' -> c - '0';
            case 'a','b','c','d','e','f' -> 10 + (c - 'a');
            case 'A','B','C','D','E','F' -> 10 + (c - 'A');
            default -> 0;
        };
    }

    protected static Long jLong(String json, String key) {
        if (json == null) return null;
        String token = "\"" + key + "\"";
        int i = json.indexOf(token);
        if (i < 0) return null;
        int c = json.indexOf(':', i + token.length());
        if (c < 0) return null;
        int s = c + 1;
        // Skip whitespace, then parse number
        while (s < json.length() && Character.isWhitespace(json.charAt(s))) s++;
        int e = s;
        while (e < json.length() && "-0123456789".indexOf(json.charAt(e)) >= 0) {
            e++;
        }
        if (s == e) return null;
        try {
            return Long.parseLong(json.substring(s, e));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public List<Long> jLongList(String json, String key) {
        String k = "\"" + key + "\"";
        int i = json.indexOf(k); if (i < 0) return List.of();
        int c = json.indexOf(':', i + k.length()); if (c < 0) return List.of();
        int a1 = json.indexOf('[', c + 1); if (a1 < 0) return List.of();
        int a2 = json.indexOf(']', a1 + 1); if (a2 < 0) return List.of();
        String arr = json.substring(a1 + 1, a2);
        List<Long> out = new java.util.ArrayList<>();
        for (String s : arr.split(",")) {
            s = s.trim();
            if (!s.isEmpty()) try { out.add(Long.parseLong(s)); } catch (Exception ignore) {}
        }
        return out;
    }

    /** Convert a collection of strings into a JSON array representation. */
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
