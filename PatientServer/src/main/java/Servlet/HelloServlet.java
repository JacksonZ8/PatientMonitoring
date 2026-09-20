package Servlet;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

// only for testing, it says hello
// https://bioeng-bbb-app.impaas.uk/hello
@WebServlet(urlPatterns = {"/hello"})
public class HelloServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        // Never echo environment variables / credentials back to the caller.
        resp.setContentType("text/plain");
        resp.getWriter().write("ok");
    }
}
