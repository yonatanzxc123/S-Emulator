// java
package server_core;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(name = "AuthServlet", urlPatterns = {"/api/login"}, loadOnStartup = 1)
public class AuthServlet extends BaseApiServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String body = readBody(req);
        String username = jStr(body, "username");
        if (username == null || username.isBlank()) {
            json(resp, 400, "{\"error\":\"missing_username\"}");
            return;
        }
        username = username.trim();

        if (USERS.containsKey(username)) {
            json(resp, 409, "{\"error\":\"username_taken\"}");
            return;
        }

        User u = new User(username);
        USERS.put(username, u);
        req.getSession(true).setAttribute("username", username);
        VERSION.incrementAndGet();

        json(resp, 200, "{\"ok\":true,\"username\":\"" + esc(username) + "\",\"credits\":" + u.credits.get() + "}");
    }
}
