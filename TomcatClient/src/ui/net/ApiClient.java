// java
// File: 'TomcatClient/src/ui/net/ApiClient.java'
package ui.net;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ApiClient {
    private final String base; // e.g. http://localhost:8080/server_Web_exploded
    private final HttpClient client;
    private final boolean debug;

    public ApiClient(String baseUrl) {
        this(baseUrl, false);
    }

    public ApiClient(String baseUrl, boolean debug) {
        String b = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
        this.base = b.isEmpty() ? "http://localhost:8080" : b;
        CookieManager cm = new CookieManager();
        cm.setCookiePolicy(CookiePolicy.ACCEPT_ALL); // keep JSESSIONID
        this.client = HttpClient.newBuilder().cookieHandler(cm).build();
        this.debug = debug;
    }

    private URI url(String path) {
        String p = (path == null || path.isBlank()) ? "/" : (path.startsWith("/") ? path : "/" + path);
        return URI.create(base + p);
    }

    private HttpResponse<String> send(HttpRequest req) throws IOException, InterruptedException {
        if (debug) System.out.println("HTTP " + req.method() + " " + req.uri());
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (debug) System.out.println("-> " + resp.statusCode() + " " + resp.body());
        return resp;
    }

    public LoginResult login(String username) {
        try {
            String body = "{\"username\":\"" + esc(username) + "\"}";
            HttpRequest req = HttpRequest.newBuilder(url("/api/login"))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> resp = send(req);
            int code = resp.statusCode();
            if (code == 200) return LoginResult.ok();
            if (code == 409) return LoginResult.fail("Username is taken");
            if (code == 400) return LoginResult.fail("Missing or invalid username");
            if (code == 401) return LoginResult.fail("Not authorized");
            return LoginResult.fail("Server error (" + code + ")");
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return LoginResult.fail("Network error: " + e.getMessage());
        }
    }

    public boolean health() {
        try {
            HttpRequest req = HttpRequest.newBuilder(url("/api/health")).GET().build();
            return send(req).statusCode() == 200;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").trim();
    }

    public static final class LoginResult {
        public final boolean success;
        public final String error;
        private LoginResult(boolean success, String error) { this.success = success; this.error = error; }
        public static LoginResult ok() { return new LoginResult(true, null); }
        public static LoginResult fail(String err) { return new LoginResult(false, err); }
    }
}
