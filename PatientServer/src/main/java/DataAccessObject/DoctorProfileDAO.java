package DataAccessObject;

import DataBase.DatabaseConnection;
import Models.DoctorProfile;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DoctorProfileDAO {
    public static DoctorProfile findByEmail(String email) {
        if (email == null || email.isBlank()) return null;

        String sql = "SELECT email, given_name, family_name, id_number, age, organization, role " +
                "FROM doctors WHERE email = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                DoctorProfile profile = new DoctorProfile();
                profile.setEmail(rs.getString("email"));
                profile.setFirstName(rs.getString("given_name"));
                profile.setLastName(rs.getString("family_name"));
                profile.setIdNumber(rs.getString("id_number"));
                profile.setAge(rs.getInt("age"));
                profile.setOrganization(rs.getString("organization"));
                profile.setRole(rs.getString("role"));
                return profile;
            }
        } catch (Exception e) {
            throw new RuntimeException("findDoctorProfile failed: " + e.getMessage(), e);
        }
    }

    public static boolean update(DoctorProfile profile) {
        if (profile == null || profile.getEmail() == null || profile.getEmail().isBlank()) return false;

        String sql = "UPDATE doctors SET given_name=?, family_name=?, id_number=?, age=?, organization=?, role=? " +
                "WHERE email=?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, safe(profile.getFirstName()));
            ps.setString(2, safe(profile.getLastName()));
            ps.setString(3, safe(profile.getIdNumber()));
            ps.setInt(4, profile.getAge());
            ps.setString(5, safe(profile.getOrganization()));
            ps.setString(6, safe(profile.getRole()));
            ps.setString(7, profile.getEmail().trim());
            return ps.executeUpdate() == 1;
        } catch (Exception e) {
            throw new RuntimeException("updateDoctorProfile failed: " + e.getMessage(), e);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
