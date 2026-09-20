package Servlet;

import DataAccessObject.DoctorDAO;
import Models.Doctor;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Authentication / authorization gate for the patient data API.
 *
 * <p>All {@code /api/*} requests must either:
 * <ul>
 *   <li>carry a valid session token ({@code Authorization: Bearer <token>}) issued by
 *       {@code /login} — the caller is then scoped to their own doctor identity, or</li>
 *   <li>explicitly target the public demo data set ({@code doctor=demo}).</li>
 * </ul>
 *
 * <p>The authenticated doctor email is exposed to downstream servlets through the
 * {@link #AUTH_EMAIL_ATTR} request attribute, so servlets never have to trust a
 * client-supplied {@code doctor}/{@code email} parameter.
 */
@WebFilter(urlPatterns = {"/api/*"})
public class AuthFilter implements Filter {

    /** Request attribute holding the authenticated doctor email (or "demo"). */
    public static final String AUTH_EMAIL_ATTR = "authDoctorEmail";

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String DEMO = "demo";

    @Override
    public void init(FilterConfig filterConfig) {
        // no initialization required
    }

    @Override
    public void destroy() {
        // no cleanup required
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // Buffer the body so it can be inspected here and still be read by the servlet.
        CachedBodyRequest cached = new CachedBodyRequest(req);

        String token = extractToken(req);
        String requested = extractRequestedOwner(cached);

        if (token != null && !token.isBlank()) {
            Doctor d = DoctorDAO.findBySessionToken(token);
            if (d == null) {
                writeError(resp, 401, "Invalid or expired session");
                return;
            }
            String owner = d.getEmail();
            if (requested != null && requested.equalsIgnoreCase(DEMO)) {
                // An explicit demo request from an authenticated user serves demo data.
                req.setAttribute(AUTH_EMAIL_ATTR, DEMO);
            } else if (requested != null && !requested.equalsIgnoreCase(owner)) {
                writeError(resp, 403, "Forbidden: you can only access your own data");
                return;
            } else {
                req.setAttribute(AUTH_EMAIL_ATTR, owner);
            }
            chain.doFilter(cached, response);
            return;
        }

        // No token: only the public demo data set may be accessed.
        if (requested != null && requested.equalsIgnoreCase(DEMO)) {
            req.setAttribute(AUTH_EMAIL_ATTR, DEMO);
            chain.doFilter(cached, response);
            return;
        }

        writeError(resp, 401, "Authentication required");
    }

    private static String extractToken(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }

    /**
     * Resolve the doctor identity the request claims to act on, from an explicit
     * {@code doctor}/{@code email} query parameter or from the JSON body.
     */
    private static String extractRequestedOwner(HttpServletRequest req) {
        String param = req.getParameter("doctor");
        if (param == null || param.isBlank()) param = req.getParameter("email");
        if (param != null && !param.isBlank()) return param.trim();

        if ("POST".equalsIgnoreCase(req.getMethod())) {
            try {
                String body = readBody(req);
                if (body != null && !body.isBlank()) {
                    JsonObject o = new JsonParser().parse(body).getAsJsonObject();
                    for (String key : new String[]{"doctor", "email"}) {
                        if (o.has(key) && !o.get(key).isJsonNull()) {
                            String v = o.get(key).getAsString();
                            if (v != null && !v.isBlank()) return v.trim();
                        }
                    }
                }
            } catch (Exception ignored) {
                // Malformed body — let the target servlet report a proper error.
            }
        }
        return null;
    }

    private static String readBody(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = req.getReader()) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    private static void writeError(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write("{\"ok\":false,\"error\":\"" + message + "\"}");
    }
}

/**
 * Wraps a request so its body can be read multiple times (once by {@link AuthFilter},
 * once by the target servlet).
 */
class CachedBodyRequest extends HttpServletRequestWrapper {

    private final byte[] body;

    CachedBodyRequest(HttpServletRequest request) throws IOException {
        super(request);
        this.body = readAll(request);
    }

    private static byte[] readAll(HttpServletRequest request) throws IOException {
        try (InputStream in = request.getInputStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(body), StandardCharsets.UTF_8));
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream buffer = new ByteArrayInputStream(body);
        return new ServletInputStream() {
            @Override public int read() { return buffer.read(); }
            @Override public boolean isFinished() { return buffer.available() == 0; }
            @Override public boolean isReady() { return true; }
            @Override public void setReadListener(ReadListener listener) { /* no-op */ }
        };
    }
}
