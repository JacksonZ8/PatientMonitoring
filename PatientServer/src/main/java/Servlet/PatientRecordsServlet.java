package Servlet;

import DataAccessObject.PatientRecordDAO;
import Models.PatientRecord;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@WebServlet(urlPatterns = {"/api/patient-records"})
public class PatientRecordsServlet extends HttpServlet {
    private static final Gson GSON = new Gson();

    private static class SaveRequest {
        String doctor;
        PatientRecord[] records;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String doctor = (String) req.getAttribute(AuthFilter.AUTH_EMAIL_ATTR);
        List<PatientRecord> records = PatientRecordDAO.listForDoctor(doctor);
        resp.getWriter().write(GSON.toJson(records));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        SaveRequest saveRequest = GSON.fromJson(readBody(req), SaveRequest.class);
        // Doctor identity comes from the authenticated session (set by AuthFilter)
        String doctor = (String) req.getAttribute(AuthFilter.AUTH_EMAIL_ATTR);
        if (doctor == null || doctor.isBlank()) doctor = "demo";
        List<PatientRecord> records = saveRequest == null || saveRequest.records == null
                ? List.of()
                : Arrays.asList(saveRequest.records);

        PatientRecordDAO.replaceForDoctor(doctor, records);

        JsonObject out = new JsonObject();
        out.addProperty("ok", true);
        out.addProperty("count", records.size());
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
