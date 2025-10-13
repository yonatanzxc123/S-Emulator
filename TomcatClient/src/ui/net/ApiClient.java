// java
package ui.net;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ApiClient {
    private static volatile ApiClient INSTANCE;

    private final String base;
    private final HttpClient client;
    private final boolean debug;

    // remember logged-in user (if any); session cookies are inside HttpClient's CookieManager
    private volatile String loggedInUser = null;

    public ApiClient(String baseUrl) { this(baseUrl, false); }

    public ApiClient(String baseUrl, boolean debug) {
        this.base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.debug = debug;
        CookieManager cm = new CookieManager();
        cm.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .cookieHandler(cm)
                .build();
        INSTANCE = this;
    }

    public static ApiClient get() {
        ApiClient x = INSTANCE;
        if (x == null) throw new IllegalStateException("ApiClient not initialized");
        return x;
    }

    // ---------- Login ----------

    public static final class LoginResult {
        public final boolean success;
        public final String error;
        public final String username;

        public LoginResult(boolean success, String error, String username) {
            this.success = success;
            this.error = error;
            this.username = username;
        }
    }

    public LoginResult login(String username, String password) throws IOException, InterruptedException {
        // Try a real auth endpoint first; fall back to /api/health if auth is not present
        String body = "{\"username\":\"" + jsonEsc(username) + "\",\"password\":\"" + jsonEsc(password) + "\"}";

        HttpRequest req = HttpRequest.newBuilder(url("/api/auth/login"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        String s = resp.body() == null ? "" : resp.body();

        if (resp.statusCode() == 404) {
            // fallback: treat server as dev mode; ping health and accept any username
            if (debug) System.out.println("[ApiClient] /api/auth/login not found; falling back to /api/health");
            HttpRequest health = HttpRequest.newBuilder(url("/api/health"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> h = client.send(health, HttpResponse.BodyHandlers.ofString());
            boolean ok = (h.statusCode() == 200) && jBool(h.body(), "ok", false);
            if (ok) {
                this.loggedInUser = username;
                return new LoginResult(true, null, username);
            }
            return new LoginResult(false, "Server is unavailable", null);
        }

        if (resp.statusCode() != 200) {
            String err = jStr(s, "error");
            String det = jStr(s, "details");
            if (det != null && !det.isBlank()) {
                err = (err == null || err.isBlank()) ? det : err + " (" + det + ")";
            }
            return new LoginResult(false, (err == null || err.isBlank()) ? ("HTTP " + resp.statusCode()) : err, null);
        }

        boolean ok = jBool(s, "ok", false);
        if (ok) {
            this.loggedInUser = username;
            return new LoginResult(true, null, username);
        }
        String err = jStr(s, "error");
        return new LoginResult(false, (err == null || err.isBlank()) ? "Login failed" : err, null);
    }

    public String currentUser() { return loggedInUser; }

    // ---------- Catalog / Programs ----------

    public static final class FunctionInfo {
        public final String name;
        public final int instr;
        public final int maxDegree;
        public FunctionInfo(String name, int instr, int maxDegree) {
            this.name = name;
            this.instr = instr;
            this.maxDegree = maxDegree;
        }
    }

    public static final class ProgramInfo {
        public final String name;
        public final String owner;
        public final int instrDeg0;
        public final int maxDegree;
        public final List<FunctionInfo> functions;
        public ProgramInfo(String name, String owner, int instrDeg0, int maxDegree, List<FunctionInfo> functions) {
            this.name = name;
            this.owner = owner;
            this.instrDeg0 = instrDeg0;
            this.maxDegree = maxDegree;
            this.functions = functions;
        }
    }

    public List<ProgramInfo> listPrograms() throws IOException, InterruptedException {
        var req = HttpRequest.newBuilder(url("/api/programs"))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        var resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) return List.of();
        String s = resp.body();
        List<ProgramInfo> out = new ArrayList<>();

        String k = "\"programs\"";
        int i = s.indexOf(k); if (i < 0) return out;
        int c = s.indexOf(':', i + k.length()); if (c < 0) return out;
        int a1 = s.indexOf('[', c + 1); if (a1 < 0) return out;
        int a2 = matchBracket(s, a1); if (a2 < 0) return out;
        String arr = s.substring(a1 + 1, a2);

        int pos = 0;
        while (pos < arr.length()) {
            int o1 = arr.indexOf('{', pos);
            if (o1 < 0) break;
            int o2 = matchBrace(arr, o1);
            if (o2 < 0) break;
            String obj = arr.substring(o1 + 1, o2);

            String name = jStr(obj, "name");
            String owner = jStr(obj, "owner");
            int instr0 = jInt(obj, "instrDeg0", 0);
            int maxDeg = jInt(obj, "maxDegree", 0);

            List<FunctionInfo> flist = new ArrayList<>();
            String fk = "\"functions\"";
            int fi = obj.indexOf(fk);
            if (fi >= 0) {
                int fc = obj.indexOf(':', fi + fk.length());
                int fa1 = obj.indexOf('[', fc + 1);
                if (fa1 >= 0) {
                    int fa2 = matchBracket(obj, fa1);
                    if (fa2 > fa1) {
                        String farr = obj.substring(fa1 + 1, fa2);
                        int fpos = 0;
                        while (fpos < farr.length()) {
                            int fo1 = farr.indexOf('{', fpos);
                            if (fo1 < 0) break;
                            int fo2 = matchBrace(farr, fo1);
                            if (fo2 < 0) break;
                            String fobj = farr.substring(fo1 + 1, fo2);

                            String fn = jStr(fobj, "name");
                            int finstr = jInt(fobj, "instr", 0);
                            int fdeg = jInt(fobj, "maxDegree", 0);
                            if (!fn.isEmpty()) {
                                flist.add(new FunctionInfo(fn, finstr, fdeg));
                            }
                            fpos = fo2 + 1;
                        }
                    }
                }
            }

            out.add(new ProgramInfo(name, owner, instr0, maxDeg, flist));
            pos = o2 + 1;
        }
        return out;
    }

    // Upload program XML and parse detailed functions
    public ProgramInfo uploadProgram(String xml) throws IOException, InterruptedException {
        String body = "{\"xml\":\"" + jsonEsc(xml) + "\"}";
        var req = HttpRequest.newBuilder(url("/api/programs/upload"))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        var resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        String s = resp.body() == null ? "" : resp.body();

        if (resp.statusCode() != 200) {
            String err = jStr(s, "error");
            String det = jStr(s, "details");
            if (det != null && !det.isBlank()) {
                err = (err == null || err.isBlank()) ? det : err + " (" + det + ")";
            }
            throw new IOException(err == null || err.isBlank() ? ("HTTP " + resp.statusCode()) : err);
        }

        String programName = jStr(s, "programName");
        String owner = jStr(s, "owner");
        int instr0 = jInt(s, "instrDeg0", 0);
        int maxDeg = jInt(s, "maxDegree", 0);

        List<FunctionInfo> flist = new ArrayList<>();
        String fk = "\"functionsDetailed\"";
        int fi = s.indexOf(fk);
        if (fi >= 0) {
            int fc = s.indexOf(':', fi + fk.length());
            int fa1 = s.indexOf('[', fc + 1);
            if (fa1 >= 0) {
                int fa2 = matchBracket(s, fa1);
                if (fa2 > fa1) {
                    String farr = s.substring(fa1 + 1, fa2);
                    int fpos = 0;
                    while (fpos < farr.length()) {
                        int fo1 = farr.indexOf('{', fpos);
                        if (fo1 < 0) break;
                        int fo2 = matchBrace(farr, fo1);
                        if (fo2 < 0) break;
                        String fobj = farr.substring(fo1 + 1, fo2);

                        String fn = jStr(fobj, "name");
                        int finstr = jInt(fobj, "instr", 0);
                        int fdeg = jInt(fobj, "maxDegree", 0);
                        if (!fn.isEmpty()) {
                            flist.add(new FunctionInfo(fn, finstr, fdeg));
                        }
                        fpos = fo2 + 1;
                    }
                }
            }
        }
        return new ProgramInfo(programName, owner, instr0, maxDeg, flist);
    }

    // --- tiny helpers for naive JSON parsing (flat keys) ---
    private static int matchBracket(String s, int openIdx) {
        int depth = 0;
        for (int i = openIdx; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '[') depth++;
            else if (ch == ']') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private static int matchBrace(String s, int openIdx) {
        int depth = 0;
        for (int i = openIdx; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{') depth++;
            else if (ch == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private static String jStr(String json, String key) {
        String k = "\"" + key + "\"";
        int i = json.indexOf(k); if (i < 0) return "";
        int c = json.indexOf(':', i + k.length()); if (c < 0) return "";
        int q1 = json.indexOf('"', c + 1); if (q1 < 0) return "";
        int q2 = json.indexOf('"', q1 + 1); if (q2 < 0) return "";
        return json.substring(q1 + 1, q2);
    }

    private static int jInt(String json, String key, int def) {
        String k = "\"" + key + "\"";
        int i = json.indexOf(k); if (i < 0) return def;
        int c = json.indexOf(':', i + k.length()); if (c < 0) return def;
        int e = c + 1;
        while (e < json.length() && Character.isWhitespace(json.charAt(e))) e++;
        int s = e;
        while (e < json.length() && "-0123456789".indexOf(json.charAt(e)) >= 0) e++;
        try { return Integer.parseInt(json.substring(s, e)); } catch (Exception ignore) { return def; }
    }

    private static boolean jBool(String json, String key, boolean def) {
        String k = "\"" + key + "\"";
        int i = json.indexOf(k); if (i < 0) return def;
        int c = json.indexOf(':', i + k.length()); if (c < 0) return def;
        int e = c + 1;
        while (e < json.length() && Character.isWhitespace(json.charAt(e))) e++;
        if (json.regionMatches(true, e, "true", 0, 4)) return true;
        if (json.regionMatches(true, e, "false", 0, 5)) return false;
        return def;
    }

    private static String jsonEsc(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 32);
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '\\' -> sb.append("\\\\");
                case '"'  -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (ch < 0x20) sb.append(String.format("\\u%04x", (int) ch));
                    else sb.append(ch);
                }
            }
        }
        return sb.toString();
    }

    private URI url(String path) {
        if (!path.startsWith("/")) path = "/" + path;
        return URI.create(base + path);
    }
}
