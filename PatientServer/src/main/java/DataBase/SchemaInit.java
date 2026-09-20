package DataBase;

import java.sql.Connection;
import java.sql.Statement;

public class SchemaInit {

    public static void ensureSchema(Connection conn) {
        try (Statement st = conn.createStatement()) {

            // Create tables if missing
            st.execute(
                    "CREATE TABLE IF NOT EXISTS patients (" +
                            "  id SERIAL PRIMARY KEY," +
                            "  doctor TEXT NOT NULL DEFAULT 'demo'," +
                            "  given_name TEXT NOT NULL," +
                            "  family_name TEXT NOT NULL," +
                            "  gender TEXT," +
                            "  age INT," +
                            "  blood_pressure TEXT" +
                            ")"
            );

            st.execute(
                    "CREATE TABLE IF NOT EXISTS doctors (" +
                            "  id SERIAL PRIMARY KEY," +
                            "  email TEXT UNIQUE NOT NULL," +
                            "  given_name TEXT NOT NULL," +
                            "  family_name TEXT NOT NULL," +
                            "  password_hash TEXT NOT NULL," +
                            "  id_number TEXT," +
                            "  age INT," +
                            "  organization TEXT," +
                            "  role TEXT," +
                            "  verified BOOLEAN NOT NULL DEFAULT FALSE," +
                            "  verification_token TEXT," +
                            "  reset_token TEXT," +
                            "  reset_token_expires TIMESTAMP," +
                            "  session_token TEXT," +
                            "  session_token_expires TIMESTAMP" +
                    ")"
            );

            // Backward-compatible migrations for existing databases.
            st.execute("ALTER TABLE doctors ADD COLUMN IF NOT EXISTS id_number TEXT");
            st.execute("ALTER TABLE doctors ADD COLUMN IF NOT EXISTS session_token TEXT");
            st.execute("ALTER TABLE doctors ADD COLUMN IF NOT EXISTS session_token_expires TIMESTAMP");
            st.execute("ALTER TABLE doctors ADD COLUMN IF NOT EXISTS age INT");
            st.execute("ALTER TABLE doctors ADD COLUMN IF NOT EXISTS organization TEXT");
            st.execute("ALTER TABLE doctors ADD COLUMN IF NOT EXISTS role TEXT");

            st.execute(
                    "CREATE TABLE IF NOT EXISTS patient_records (" +
                            "  id SERIAL PRIMARY KEY," +
                            "  doctor TEXT NOT NULL DEFAULT 'demo'," +
                            "  patient_name TEXT NOT NULL," +
                            "  record_id TEXT NOT NULL," +
                            "  diagnosis TEXT NOT NULL," +
                            "  record_date TEXT NOT NULL," +
                            "  created_at TIMESTAMP NOT NULL DEFAULT now()" +
                            ")"
            );

            // Index check to make it solid
            st.execute("CREATE INDEX IF NOT EXISTS idx_doctors_email ON doctors(email)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_patients_doctor ON patients(doctor)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_patient_records_doctor ON patient_records(doctor)");

        } catch (Exception e) {
            throw new RuntimeException("Schema init failed", e);
        }
    }
}
