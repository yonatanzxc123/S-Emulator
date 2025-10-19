// java
package ui.net;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public class ApiClient {
    private static volatile ApiClient INSTANCE;

    private final String base;
    private final HttpClient client;
    private final boolean debug;
    private final Preferences prefs;

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

        this.prefs = Preferences.userNodeForPackage(ApiClient.class);
        this.loggedInUser = prefs.get("lastUsername", null); // keep across restarts

        INSTANCE = this;
    }
    public static final class UserOnline {
        public final String name;
        public final long mainUploaded;
        public final long helperContrib;
        public final long credits;
        public final long creditsSpent;
        public final long runsCount;

        public UserOnline(String name, long mainUploaded, long helperContrib,
                          long credits, long creditsSpent, long runsCount) {
            this.name = name;
            this.mainUploaded = mainUploaded;
            this.helperContrib = helperContrib;
            this.credits = credits;
            this.creditsSpent = creditsSpent;
            this.runsCount = runsCount;
        }
    }

    public List<UserOnline> usersOnline() throws java.io.IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(url("/api/users/online"))
                .timeout(java.time.Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) return List.of();
        String s = resp.body() == null ? "" : resp.body();

        List<UserOnline> out = new ArrayList<>();
        String k = "\"users\"";
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
            long mainUploaded = jLong(obj, "mainUploaded", 0L);
            long helperContrib = jLong(obj, "helperContrib", 0L);
            long credits = jLong(obj, "credits", 0L);
            long creditsSpent = jLong(obj, "creditsSpent", 0L);
            long runsCount = jLong(obj, "runsCount", 0L);

            if (name != null) {
                out.add(new UserOnline(name, mainUploaded, helperContrib, credits, creditsSpent, runsCount));
            }
            pos = o2 + 1;
        }
        return out;
    }
    public static final class ProgramInstruction {
        public final int index;
        public final String op;
        public final String level;
        public final String bs;
        public final String label;
        public final int cycles;

        public ProgramInstruction(int index, String op, String level, String bs, String label, int cycles) {
            this.index = index;
            this.op = op;
            this.level = level;
            this.bs = bs;
            this.label = label;
            this.cycles = cycles;
        }
        public int getIndex()   { return index; }
        public String getOp()   { return op; }
        public String getLevel(){ return level; }
        public String getBs()   { return bs; }
        public String getLabel(){ return label; }
        public int getCycles()  { return cycles; }
    }

    private static String encSeg(String s) {
        if (s == null) return "null";
        String q = URLEncoder.encode(s, StandardCharsets.UTF_8);
        return q.replace("+", "%20");
    }


    public List<ProgramInstruction> programBody(String programName) throws IOException, InterruptedException {
        String path = "/api/programs/" + encSeg(programName) + "/body";
        HttpRequest req = HttpRequest.newBuilder(url(path))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        var resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            if (debug) System.err.println("GET " + path + " -> " + resp.statusCode());
            return List.of();
        }
        String s = resp.body() == null ? "" : resp.body();

        List<ProgramInstruction> out = new ArrayList<>();
        String key = "\"instructions\"";
        int i = s.indexOf(key); if (i < 0) return out;
        int c = s.indexOf(':', i + key.length()); if (c < 0) return out;
        int a1 = s.indexOf('[', c + 1); if (a1 < 0) return out;
        int a2 = matchBracket(s, a1); if (a2 < 0) return out;
        String arr = s.substring(a1 + 1, a2);

        int pos = 0;
        while (pos < arr.length()) {
            int o1 = arr.indexOf('{', pos); if (o1 < 0) break;
            int o2 = matchBrace(arr, o1); if (o2 < 0) break;
            String obj = arr.substring(o1 + 1, o2);

            int idx = jInt(obj, "index", -1);
            String op  = jStr(obj, "op");
            String lvl = jStr(obj, "level");
            String bs  = jStr(obj, "bs");
            String lbl = jStr(obj, "label");
            int cyc    = jInt(obj, "cycles", 0);

            if (bs == null || bs.isBlank()) bs = ("I".equals(lvl) ? "B" : "S");
            if (idx >= 0 && op != null) out.add(new ProgramInstruction(idx, op, (lvl == null ? "" : lvl), bs, (lbl == null ? "" : lbl), Math.max(0, cyc)));
            pos = o2 + 1;
        }
        return out;
    }

    public List<ProgramInstruction> programBody(String programName, int degree) throws IOException, InterruptedException {
        String path = "/api/programs/" + encSeg(programName) + "/body?degree=" + Math.max(0, degree);
        HttpRequest req = HttpRequest.newBuilder(url(path))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            if (debug) System.err.println("GET " + path + " -> " + resp.statusCode());
            return List.of();
        }
        String s = resp.body() == null ? "" : resp.body();

        List<ProgramInstruction> out = new ArrayList<>();
        String key = "\"instructions\"";
        int i = s.indexOf(key); if (i < 0) return out;
        int c = s.indexOf(':', i + key.length()); if (c < 0) return out;
        int a1 = s.indexOf('[', c + 1); if (a1 < 0) return out;
        int a2 = matchBracket(s, a1); if (a2 < 0) return out;
        String arr = s.substring(a1 + 1, a2);

        int pos = 0;
        while (pos < arr.length()) {
            int o1 = arr.indexOf('{', pos); if (o1 < 0) break;
            int o2 = matchBrace(arr, o1); if (o2 < 0) break;
            String obj = arr.substring(o1 + 1, o2);

            int idx = jInt(obj, "index", -1);
            String op  = jStr(obj, "op");
            String lvl = jStr(obj, "level");
            String bs  = jStr(obj, "bs");
            String lbl = jStr(obj, "label");
            int cyc    = jInt(obj, "cycles", 0);

            if (bs == null || bs.isBlank()) bs = ("I".equals(lvl) ? "B" : "S");
            if (idx >= 0 && op != null) out.add(new ProgramInstruction(idx, op, (lvl == null ? "" : lvl), bs, (lbl == null ? "" : lbl), Math.max(0, cyc)));
            pos = o2 + 1;
        }
        return out;
    }

    public int programMaxDegree(String programName) throws IOException, InterruptedException {
        String path = "/api/programs/" + encSeg(programName) + "/body";
        HttpRequest req = HttpRequest.newBuilder(url(path))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            if (debug) System.err.println("GET " + path + " -> " + resp.statusCode());
            return 0;
        }
        String s = resp.body() == null ? "" : resp.body();
        return jInt(s, "maxDegree", 0);
    }

    public static final class FunctionSummary {
        public final String name;
        public final String program;
        public final String owner;
        public final int instr;
        public final int maxDegree;

        public FunctionSummary(String name, String program, String owner, int instr, int maxDegree) {
            this.name = name;
            this.program = program;
            this.owner = owner;
            this.instr = instr;
            this.maxDegree = maxDegree;
        }
    }

    public List<FunctionSummary> listAllFunctions() throws java.io.IOException, InterruptedException {
        var req = HttpRequest.newBuilder(url("/api/functions"))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        var resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) return List.of();
        String s = resp.body() == null ? "" : resp.body();

        List<FunctionSummary> out = new ArrayList<>();
        String k = "\"functions\"";
        int i = s.indexOf(k); if (i < 0) return out;
        int c = s.indexOf(':', i + k.length()); if (c < 0) return out;
        int a1 = s.indexOf('[', c + 1); if (a1 < 0) return out;
        int a2 = matchBracket(s, a1); if (a2 < 0) return out;
        String arr = s.substring(a1 + 1, a2);

        int pos = 0;
        while (pos < arr.length()) {
            int o1 = arr.indexOf('{', pos); if (o1 < 0) break;
            int o2 = matchBrace(arr, o1); if (o2 < 0) break;
            String obj = arr.substring(o1 + 1, o2);

            String name = jStr(obj, "name");
            String program = jStr(obj, "program");
            String owner = jStr(obj, "owner");
            int instr = jInt(obj, "instr", 0);
            int maxDeg = jInt(obj, "maxDegree", 0);

            if (name != null && program != null) {
                out.add(new FunctionSummary(name, program, owner == null ? "" : owner, instr, maxDeg));
            }
            pos = o2 + 1;
        }
        return out;
    }

    // POST /api/users/credits/add
    public long addCredits(long amount) throws java.io.IOException, InterruptedException {
        String body = "{\"amount\":" + amount + "}";
        HttpRequest req = HttpRequest.newBuilder(url("/api/users/credits/add"))
                .timeout(java.time.Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        String s = resp.body() == null ? "" : resp.body();
        if (resp.statusCode() != 200) throw new java.io.IOException("HTTP " + resp.statusCode());
        return jLong(s, "credits", 0L);
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
        String body = "{\"username\":\"" + jsonEsc(username) + "\",\"password\":\"" + jsonEsc(password) + "\"}";

        HttpRequest req = HttpRequest.newBuilder(url("/api/auth/login"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        String s = resp.body() == null ? "" : resp.body();

        if (resp.statusCode() != 200) {
            String err = jStr(s, "error");
            return new LoginResult(false, (err == null || err.isBlank()) ? ("HTTP " + resp.statusCode()) : err, null);
        }

        boolean ok = jBool(s, "ok", false);
        if (ok) {
            this.loggedInUser = username;
            prefs.put("lastUsername", username);
            return new LoginResult(true, null, username);
        }
        String err = jStr(s, "error");
        return new LoginResult(false, (err == null || err.isBlank()) ? "Login failed" : err, null);
    }

    // Invalidate server session; by default keep the remembered username locally.
    public void logout() throws IOException, InterruptedException {
        logout(true);
    }

    public void logout(boolean keepUsername) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(url("/api/auth/logout"))
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        client.send(req, HttpResponse.BodyHandlers.discarding());

        if (!keepUsername) {
            this.loggedInUser = null;
            prefs.remove("lastUsername");
        }
    }

    // Returns the last known username (may come from preferences; not proof of an active session)
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
            this.functions = functions == null ? List.of() : List.copyOf(functions);
        }
    }

    public List<ProgramInfo> listPrograms() throws IOException, InterruptedException {
        var req = HttpRequest.newBuilder(url("/api/programs"))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        var resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) return List.of();
        String s = resp.body() == null ? "" : resp.body();

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
                            if (fn != null && !fn.isBlank()) {
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
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        var resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        String s = resp.body() == null ? "" : resp.body();

        if (resp.statusCode() != 200) {
            String err = jStr(s, "error");
            throw new IOException((err == null || err.isBlank()) ? ("HTTP " + resp.statusCode()) : err);
        }

        boolean ok = jBool(s, "ok", false);
        if (!ok) {
            String err = jStr(s, "error");
            throw new IOException((err == null || err.isBlank()) ? "Upload failed" : err);
        }

        String name = jStr(s, "programName");
        String owner = jStr(s, "owner");
        int instr0 = jInt(s, "instrDeg0", 0);
        int maxDeg = jInt(s, "maxDegree", 0);

        List<FunctionInfo> flist = new ArrayList<>();
        // Prefer detailed functions
        String key = "\"functionsDetailed\"";
        int i = s.indexOf(key);
        if (i >= 0) {
            int c = s.indexOf(':', i + key.length());
            int a1 = s.indexOf('[', c + 1);
            if (a1 >= 0) {
                int a2 = matchBracket(s, a1);
                if (a2 > a1) {
                    String arr = s.substring(a1 + 1, a2);
                    int pos = 0;
                    while (pos < arr.length()) {
                        int o1 = arr.indexOf('{', pos);
                        if (o1 < 0) break;
                        int o2 = matchBrace(arr, o1);
                        if (o2 < 0) break;
                        String obj = arr.substring(o1 + 1, o2);
                        String fn = jStr(obj, "name");
                        int finstr = jInt(obj, "instr", 0);
                        int fdeg = jInt(obj, "maxDegree", 0);
                        if (fn != null && !fn.isBlank()) {
                            flist.add(new FunctionInfo(fn, finstr, fdeg));
                        }
                        pos = o2 + 1;
                    }
                }
            }
        }
        return new ProgramInfo(name, owner, instr0, maxDeg, flist);
    }

    // --- tiny helpers for naive JSON parsing (flat keys) ---
    private static int matchBracket(String s, int openIdx) {
        int depth = 0;
        for (int i = openIdx; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '[') depth++;
            else if (ch == ']') { depth--; if (depth == 0) return i; }
        }
        return -1;
    }

    private static int matchBrace(String s, int openIdx) {
        int depth = 0;
        for (int i = openIdx; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{') depth++;
            else if (ch == '}') { depth--; if (depth == 0) return i; }
        }
        return -1;
    }

    private static String jStr(String json, String key) {
        if (json == null) return null;
        String k = "\"" + key + "\"";
        int i = json.indexOf(k); if (i < 0) return null;
        int c = json.indexOf(':', i + k.length()); if (c < 0) return null;
        int p = c + 1, n = json.length();
        while (p < n && Character.isWhitespace(json.charAt(p))) p++;
        if (p >= n || json.charAt(p) != '"') return null;

        boolean esc = false;
        int q1 = p;
        for (int q = p + 1; q < n; q++) {
            char ch = json.charAt(q);
            if (esc) { esc = false; continue; }
            if (ch == '\\') { esc = true; continue; }
            if (ch == '"') return json.substring(q1 + 1, q);
        }
        return null;
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
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (ch < 0x20) {
                        sb.append(String.format("\\u%04x", (int) ch));
                    } else {
                        sb.append(ch);
                    }
            }
        }
        return sb.toString();
    }

    private static long jLong(String json, String key, long def) {
        String k = "\"" + key + "\"";
        int i = json.indexOf(k); if (i < 0) return def;
        int c = json.indexOf(':', i + k.length()); if (c < 0) return def;
        int e = c + 1;
        while (e < json.length() && Character.isWhitespace(json.charAt(e))) e++;
        int s = e;
        while (e < json.length() && "-0123456789".indexOf(json.charAt(e)) >= 0) e++;
        try { return Long.parseLong(json.substring(s, e)); } catch (Exception ignore) { return def; }
    }

    private URI url(String path) {
        if (!path.startsWith("/")) path = "/" + path;
        return URI.create(base + path);
    }
}
