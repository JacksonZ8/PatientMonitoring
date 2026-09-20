package Servlet;

import DataAccessObject.DoctorDAO;
import Models.Doctor;
import org.mindrot.jbcrypt.BCrypt;

import com.google.gson.Gson;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/resetPassword")
public class ResetPasswordServlet extends HttpServlet {

    private static class ResetPasswordReq {
        String token;
        String newPassword;
    }

    private static class SimpleResponse {
        String status;
        String message;
        SimpleResponse(String status, String message) {
            this.status = status; this.message = message;
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("application/json");
        Gson gson = new Gson();

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        ResetPasswordReq r = gson.fromJson(sb.toString(), ResetPasswordReq.class);

        PrintWriter out = resp.getWriter();

        Doctor d = DoctorDAO.findByResetToken(r.token);
        if (d == null) {
            out.println(gson.toJson(new SimpleResponse("error", "Invalid or expired token")));
            return;
        }

        String newHash = BCrypt.hashpw(r.newPassword, BCrypt.gensalt(12));
        DoctorDAO.updatePassword(d.getId(), newHash);

        out.println(gson.toJson(new SimpleResponse("ok", "Password updated")));
    }
}
