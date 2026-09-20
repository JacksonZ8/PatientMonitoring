package DataAccessObject;

import DataBase.DatabaseConnection;
import Models.PatientRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class PatientRecordDAO {
    public static List<PatientRecord> listForDoctor(String doctor) {
        if (doctor == null || doctor.isBlank()) doctor = "demo";

        String sql = "SELECT patient_name, record_id, diagnosis, record_date " +
                "FROM patient_records WHERE doctor=? ORDER BY id";
        List<PatientRecord> records = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, doctor.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PatientRecord record = new PatientRecord();
                    record.setPatientName(rs.getString("patient_name"));
                    record.setRecordId(rs.getString("record_id"));
                    record.setDiagnosis(rs.getString("diagnosis"));
                    record.setDate(rs.getString("record_date"));
                    records.add(record);
                }
            }
            return records;
        } catch (Exception e) {
            throw new RuntimeException("listPatientRecords failed: " + e.getMessage(), e);
        }
    }

    public static void replaceForDoctor(String doctor, List<PatientRecord> records) {
        if (doctor == null || doctor.isBlank()) doctor = "demo";
        final String owner = doctor.trim();

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement del = conn.prepareStatement("DELETE FROM patient_records WHERE doctor=?")) {
                    del.setString(1, owner);
                    del.executeUpdate();
                }

                if (records != null && !records.isEmpty()) {
                    String sql = "INSERT INTO patient_records " +
                            "(doctor, patient_name, record_id, diagnosis, record_date) VALUES (?,?,?,?,?)";
                    try (PreparedStatement ins = conn.prepareStatement(sql)) {
                        for (PatientRecord record : records) {
                            ins.setString(1, owner);
                            ins.setString(2, safe(record.getPatientName()));
                            ins.setString(3, safe(record.getRecordId()));
                            ins.setString(4, safe(record.getDiagnosis()));
                            ins.setString(5, safe(record.getDate()));
                            ins.addBatch();
                        }
                        ins.executeBatch();
                    }
                }

                conn.commit();
            } catch (Exception e) {
                try { conn.rollback(); } catch (Exception ignored) {}
                throw e;
            } finally {
                try { conn.setAutoCommit(true); } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            throw new RuntimeException("replacePatientRecords failed: " + e.getMessage(), e);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
