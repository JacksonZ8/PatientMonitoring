package Servlet;

import DataAccessObject.DoctorProfileDAO;
import Models.DoctorProfile;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

@WebServlet(urlPatterns = {"/api/doctor-profile"})
public class DoctorProfileServlet extends HttpServlet {
    private static final Gson GSON = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String email = req.getParameter("email");
        if (email == null || email.isBlank() || "demo".equalsIgnoreCase(email.trim())) {
            resp.setStatus(400);
            resp.getWriter().write("{\"ok\":false,\"error\":\"Missing real doctor email\"}");
            return;
        }

        DoctorProfile profile = DoctorProfileDAO.findByEmail(email);
        if (profile == null) {
            resp.setStatus(404);
            resp.getWriter().write("{\"ok\":false,\"error\":\"Doctor not found\"}");
            return;
        }

        resp.getWriter().write(GSON.toJson(profile));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        DoctorProfile profile = GSON.fromJson(readBody(req), DoctorProfile.class);
        if (profile == null || profile.getEmail() == null || profile.getEmail().isBlank()
                || "demo".equalsIgnoreCase(profile.getEmail().trim())) {
            resp.setStatus(400);
            resp.getWriter().write("{\"ok\":false,\"error\":\"Missing real doctor email\"}");
            return;
        }

        boolean updated = DoctorProfileDAO.update(profile);
        JsonObject out = new JsonObject();
        out.addProperty("ok", updated);
        if (!updated) {
            resp.setStatus(404);
            out.addProperty("error", "Doctor not found");
        }
        resp.getWriter().write(out.toString());
    }

    private static String readBody(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = req.getReader()) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }
}
