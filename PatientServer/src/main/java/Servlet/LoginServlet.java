package Servlet;

import com.google.gson.Gson;
import DataAccessObject.DoctorDAO;
import Models.Doctor;
import org.mindrot.jbcrypt.BCrypt;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private static class LoginRequest {
        String email;
        String password;
    }

    private static class LoginResponse {
        String status;
        String message;
        Integer doctorId;
        String givenName;
        String familyName;
        LoginResponse(String status, String message) {
            this.status = status;
            this.message = message;
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
        LoginRequest loginReq = gson.fromJson(sb.toString(), LoginRequest.class);

        PrintWriter out = resp.getWriter();

        Doctor d = DoctorDAO.findByEmail(loginReq.email);
        if (d == null) {
            out.println(gson.toJson(new LoginResponse("error", "Email not found")));
            return;
        }

        // Verify password against the stored bcrypt hash
        boolean passwordMatches;
        try {
            passwordMatches = BCrypt.checkpw(loginReq.password, d.getPasswordHash());
        } catch (Exception e) {
            passwordMatches = false;
        }
        if (!passwordMatches) {
            out.println(gson.toJson(new LoginResponse("error", "Wrong password")));
            return;
        }

        if (!d.isVerified()) {
            out.println(gson.toJson(new LoginResponse("error", "Please verify your email first")));
            return;
        }

        LoginResponse res = new LoginResponse("ok", "Login successful");
        res.doctorId = d.getId();
        res.givenName = d.getGivenName();
        res.familyName = d.getFamilyName();
        out.println(gson.toJson(res));
    }
}
