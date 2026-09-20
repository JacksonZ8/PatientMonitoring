package NetWork;

// Holds current logged-in doctor info (from database)
public final class Session {
    // Logged-in doctor email. Empty means not logged in.
    private static volatile String doctorEmail = "";
    // Cached doctor profile fields
    private static volatile String doctorGivenName = "";
    private static volatile String doctorFamilyName = "";
    private static volatile String doctorRole = "";

    private Session() {}

    // Set doctor email after login
    public static void setDoctorEmail(String email) {
        if (email != null && !email.isBlank()) {
            doctorEmail = email.trim();
        }
    }

    // Set doctor's given and family name
    public static void setDoctorName(String givenName, String familyName) {
        doctorGivenName = (givenName == null ? "" : givenName.trim());
        doctorFamilyName = (familyName == null ? "" : familyName.trim());
    }

    // Prefer full name, fallback to email
    public static String getDoctorFullName() {
        String full = (doctorGivenName + " " + doctorFamilyName).trim();
        return full.isBlank() ? getDoctorEmail() : full;
    }

    // Set doctor role
    public static void setDoctorRole(String role) {
        doctorRole = (role == null ? "" : role.trim());
    }

    // Get current doctor role
    public static String getDoctorRole() {
        return doctorRole;
    }

    // Get current doctor email (falls back to "demo" when not logged in)
    public static String getDoctorEmail() {
        return (doctorEmail == null || doctorEmail.isBlank()) ? "demo" : doctorEmail;
    }

    // Raw email without fallback (useful for login checks)
    public static String getDoctorEmailRaw() {
        return doctorEmail;
    }

    public static boolean isLoggedIn() {
        return doctorEmail != null && !doctorEmail.isBlank() && !"demo".equalsIgnoreCase(doctorEmail.trim());
    }

    // Clear session on logout
    public static void clear() {
        doctorEmail = "";
        doctorGivenName = "";
        doctorFamilyName = "";
        doctorRole = "";
    }
}
