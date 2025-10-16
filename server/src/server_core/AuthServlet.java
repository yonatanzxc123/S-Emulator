// java
package server_core;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet(name = "AuthServlet", urlPatterns = {"/api/auth/*"}, loadOnStartup = 1)
public class AuthServlet extends BaseApiServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String sp = subPath(req);
        if (sp == null) sp = "";
        switch (sp) {
            case "/login" -> handleLogin(req, resp);
            case "/logout" -> handleLogout(req, resp);
            default -> json(resp, 404, "{\"error\":\"not_found\"}");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String sp = subPath(req);
        if (sp == null) sp = "";
        switch (sp) {
            case "/me" -> handleMe(req, resp);
            default -> json(resp, 404, "{\"error\":\"not_found\"}");
        }
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String body = readBody(req);
        String username = jStr(body, "username");
        if (username == null || username.isBlank()) {
            json(resp, 400, "{\"error\":\"missing_username\"}");
            return;
        }

        // Reserve the username for the lifetime of this server process.
        // If it was ever used before, reject as taken.
        User newUser = new User(username);
        User existing = USERS.putIfAbsent(username, newUser);
        if (existing != null) {
            json(resp, 409, "{\"error\":\"username_taken\"}");
            return;
        }

        // Create session and bind the reserved user
        HttpSession session = req.getSession(true); // ensures Set-Cookie: JSESSIONID
        session.setAttribute("user", newUser);

        json(resp, 200, "{\"ok\":true,\"username\":\"" + esc(username) + "\"," + "\"credits\":" + newUser.credits.get()+ "}");
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Do NOT remove from USERS; keep the name reserved until server restart.
        HttpSession ses = req.getSession(false);
        if (ses != null) ses.invalidate();
        json(resp, 200, "{\"ok\":true}");
    }

    private void handleMe(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User u = requireUser(req, resp);
        if (u == null) return; // requireUser already wrote 401
        String name = u.name();

        json(resp, 200, "{" + "\"ok\":true," + "\"username\":\"" + esc(name) + "\"," + "\"credits\":" + u.credits.get() + "}");    }
}
