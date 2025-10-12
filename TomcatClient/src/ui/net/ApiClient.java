// java
package ui.net;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ApiClient {
    private static volatile ApiClient INSTANCE;

    private final String base;
    private final HttpClient client;
    private final boolean debug;

    public ApiClient(String baseUrl) { this(baseUrl, false); }

    public ApiClient(String baseUrl, boolean debug) {
        String b = (baseUrl == null || baseUrl.isBlank())
                ? System.getProperty("api.base")
                : baseUrl;
        if (b == null || b.isBlank()) b = "http://localhost:8080/server_Web_exploded";
        this.base = trimRightSlash(b);
        this.debug = debug;

        var cm = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        this.client = HttpClient.newBuilder()
                .cookieHandler(cm)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        if (debug) System.out.println("[ApiClient] base = " + this.base);
    }

    public static ApiClient get() {
        ApiClient inst = INSTANCE;
        if (inst == null) {
            synchronized (ApiClient.class) {
                inst = INSTANCE;
                if (inst == null) INSTANCE = inst = new ApiClient(System.getProperty("api.base"), true);
            }
        }
        return inst;
    }

    private static String trimRightSlash(String s) {
        int e = s.length();
        while (e > 0 && s.charAt(e - 1) == '/') e--;
        return s.substring(0, e);
    }

    private URI url(String path) {
        String p = (path == null) ? "" : path.strip();
        if (!p.startsWith("/")) p = "/" + p;
        String full = base + p;
        if (debug) System.out.println("[ApiClient] HTTP " + full);
        return URI.create(full);
    }

    private HttpResponse<String> send(HttpRequest req) throws IOException, InterruptedException {
        return client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    // ---- Auth ----

    public static final class LoginResult {
        public final boolean success;
        public final String username;
        public final long credits;
        public final String error;

        public LoginResult(boolean success, String username, long credits, String error) {
            this.success = success;
            this.username = username;
            this.credits = credits;
            this.error = error;
        }
    }

    public LoginResult login(String username) {
        try {
            String body = "{\"username\":\"" + esc(username == null ? "" : username.trim()) + "\"}";
            var req = HttpRequest.newBuilder(url("/api/login"))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            var resp = send(req);
            int code = resp.statusCode();
            String s = resp.body();
            if (debug) System.out.println("[ApiClient] /api/login -> " + code + " " + s);

            if (code == 200) {
                String u = pickStr(s, "username");
                long credits = pickLong(s, "credits", 0L);
                return new LoginResult(true, u, credits, null);
            }
            String errCode = pickStr(s, "error");
            String msg = humanizeLoginError(errCode);
            return new LoginResult(false, null, 0L, msg != null ? msg : ("HTTP " + code));
        } catch (Exception e) {
            return new LoginResult(false, null, 0L, "Network error");
        }
    }

    private static String humanizeLoginError(String err) {
        if (err == null) return null;
        return switch (err) {
            case "missing_username" -> "Username is required";
            case "username_taken" -> "Username is already in use";
            default -> err;
        };
    }

    // ---- Programs ----

    public static final class UploadResult {
        public final boolean ok;
        public final String programName;
        public final String owner;
        public final int instrDeg0;
        public final int maxDegree;
        public final List<String> functions;
        public final String error;

        public UploadResult(boolean ok, String programName, String owner, int instrDeg0, int maxDegree, List<String> functions, String error) {
            this.ok = ok; this.programName = programName; this.owner = owner;
            this.instrDeg0 = instrDeg0; this.maxDegree = maxDegree; this.functions = functions; this.error = error;
        }
    }

    public UploadResult uploadProgram(String xml) throws IOException, InterruptedException {
        String body = "{\"xml\":\"" + esc(xml == null ? "" : xml) + "\"}";
        var req = HttpRequest.newBuilder(url("/api/programs/upload"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        var resp = send(req);
        int code = resp.statusCode();
        String s = resp.body();
        if (debug) System.out.println("[ApiClient] /api/programs/upload -> " + code + " " + s);

        if (code == 200) {
            String programName = pickNestedStr(s, "addedProgram", "name");
            String owner = pickNestedStr(s, "addedProgram", "owner");
            int instr = pickInt(s, "instrDeg0", 0);
            int maxDeg = pickInt(s, "maxDegree", 0);
            List<String> functions = pickArrayOfStrings(s, "provides");
            return new UploadResult(true, programName, owner, instr, maxDeg, functions, null);
        }
        String err = pickStr(s, "error");
        return new UploadResult(false, null, null, 0, 0, List.of(), err != null ? err : ("HTTP " + code));
    }

    // ---- Optional health check ----
    public boolean health() {
        try {
            var req = HttpRequest.newBuilder(url("/api/health"))
                    .GET()
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(5))
                    .build();
            var resp = send(req);
            if (resp.statusCode() != 200) return false;
            String body = resp.body();
            return body != null && body.contains("\"ok\":true");
        } catch (Exception e) {
            return false;
        }
    }

    // ---- JSON helpers ----

    private static String esc(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static String pickNestedStr(String json, String objKey, String innerKey) {
        if (json == null) return null;
        String ok = "\"" + objKey + "\"";
        int oi = json.indexOf(ok); if (oi < 0) return null;
        int oc = json.indexOf('{', oi); if (oc < 0) return null;
        int depth = 0, end = -1;
        for (int i = oc; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') { depth--; if (depth == 0) { end = i; break; } }
        }
        if (end < 0) return null;
        String sub = json.substring(oc, end + 1);
        return pickStr(sub, innerKey);
    }

    private static String pickStr(String json, String key) {
        if (json == null) return null;
        String k = "\"" + key + "\"";
        int i = json.indexOf(k); if (i < 0) return null;
        int c = json.indexOf(':', i + k.length()); if (c < 0) return null;
        int q1 = json.indexOf('"', c + 1); if (q1 < 0) return null;
        int q2 = json.indexOf('"', q1 + 1); if (q2 < 0) return null;
        return json.substring(q1 + 1, q2);
    }

    private static int pickInt(String json, String key, int defVal) {
        long v = pickLong(json, key, defVal);
        return (int) v;
    }

    private static long pickLong(String json, String key, long defVal) {
        if (json == null) return defVal;
        String k = "\"" + key + "\"";
        int i = json.indexOf(k); if (i < 0) return defVal;
        int c = json.indexOf(':', i + k.length()); if (c < 0) return defVal;
        int s = c + 1;
        while (s < json.length() && Character.isWhitespace(json.charAt(s))) s++;
        int e = s;
        while (e < json.length() && "-0123456789".indexOf(json.charAt(e)) >= 0) e++;
        if (s == e) return defVal;
        try { return Long.parseLong(json.substring(s, e)); } catch (Exception ignored) { return defVal; }
    }

    private static List<String> pickArrayOfStrings(String json, String key) {
        List<String> out = new ArrayList<>();
        if (json == null) return out;
        String k = "\"" + key + "\"";
        int i = json.indexOf(k); if (i < 0) return out;
        int c = json.indexOf(':', i + k.length()); if (c < 0) return out;
        int a1 = json.indexOf('[', c + 1); if (a1 < 0) return out;
        int a2 = json.indexOf(']', a1 + 1); if (a2 < 0) return out;
        String arr = json.substring(a1 + 1, a2);
        for (String part : arr.split(",")) {
            part = part.trim();
            if (part.startsWith("\"") && part.endsWith("\"") && part.length() >= 2) {
                out.add(part.substring(1, part.length() - 1)
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\"));
            }
        }
        return out;
    }
}
