package Servlet;

import com.google.gson.Gson;
import DataAccessObject.DoctorDAO;
import Utils.EmailSender;
import jakarta.mail.MessagingException;
import org.mindrot.jbcrypt.BCrypt;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private static class RegisterRequest {
        String email;
        String password;
        String givenName;
        String familyName;
    }

    private static class SimpleResponse {
        String status;
        String message;
        SimpleResponse(String status, String message) {
            this.status = status;
            this.message = message;
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("application/json");
        Gson gson = new Gson();

        // 1. Read JSON body
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        RegisterRequest registerReq = gson.fromJson(sb.toString(), RegisterRequest.class);

        PrintWriter out = resp.getWriter();

        // Basic validation to avoid blank input
        if (registerReq == null || registerReq.email == null || registerReq.password == null ||
                registerReq.givenName == null || registerReq.familyName == null ||
                registerReq.email.isBlank() || registerReq.password.isBlank() ||
                registerReq.givenName.isBlank() || registerReq.familyName.isBlank()) {
            resp.setStatus(400);
            resp.getWriter().println(gson.toJson(new SimpleResponse("error", "Missing required fields")));
            return;
        }

        // 2. Check if email already exists
        var existing = DoctorDAO.findByEmail(registerReq.email);
        if (existing != null) {
            out.println(gson.toJson(new SimpleResponse("error", "Email already registered")));
            return;
        }

        // 3. Hash password with bcrypt (salted, adaptive)
        String passwordHash = BCrypt.hashpw(registerReq.password, BCrypt.gensalt(12));

        // 4. Generate verification token
        String token = UUID.randomUUID().toString();

        // 5. Insert doctor (starts unverified; login enforces verification)
        int newDoctorId;
        try {
            newDoctorId = DoctorDAO.insertDoctor(
                    registerReq.email,
                    registerReq.givenName,
                    registerReq.familyName,
                    passwordHash,
                    token
            );
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(500);
            out.println(gson.toJson(new SimpleResponse("error", "Registration failed. Please try again later.")));
            return;
        }

        // 6. Email verification
        // Dev/demo convenience: set SKIP_EMAIL_VERIFICATION=true to bypass email
        // and auto-verify. Production should configure SMTP_* and leave it unset.
        boolean skipVerification = "true".equalsIgnoreCase(System.getenv("SKIP_EMAIL_VERIFICATION"));
        if (skipVerification) {
            DoctorDAO.markVerified(newDoctorId);
            out.println(gson.toJson(new SimpleResponse("ok", "Registration successful.")));
            return;
        }

        String appBaseUrl = System.getenv("APP_BASE_URL");
        if (appBaseUrl == null || appBaseUrl.isBlank()) {
            appBaseUrl = "http://localhost:8080/PatientServer";
        }
        String verifyLink = appBaseUrl + "/verifyEmail?token=" + token;

        String subject = "Verify your HealthTrack account";
        String body = "Hi " + registerReq.givenName + ",\n\n" +
                "Please click the link below to verify your account:\n" +
                verifyLink + "\n\n" +
                "If you did not register, ignore this email.";

        try {
            EmailSender.sendEmail(registerReq.email, subject, body);
            out.println(gson.toJson(new SimpleResponse("ok", "Registration successful. Check your email to verify your account.")));
        } catch (MessagingException e) {
            // SMTP not configured / send failed: cannot verify by email.
            // Fail-safe so the app stays usable, but log loudly for operators.
            System.err.println("WARN: verification email failed to send: " + e.getMessage()
                    + " — auto-verifying as fallback (set SMTP_* and remove SKIP_EMAIL_VERIFICATION for production)");
            DoctorDAO.markVerified(newDoctorId);
            out.println(gson.toJson(new SimpleResponse("ok", "Registration successful.")));
        }
    }

    // testing
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("text/plain");
        resp.getWriter().write("Hello from /register on Tsuru!");
    }
}
