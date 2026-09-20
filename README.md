# PatientMonitoring

A Remote Patient Monitoring (RPM) system — developed as a biomedical-engineering
software project. Doctors can manage patients and monitor simulated vital signs
(ECG, respiration, SpO₂, temperature, blood pressure) with waveform and
digital-twin visualizations, while a servlet backend persists data in PostgreSQL.

This repository is a monorepo containing the two cooperating sub-projects.

## Structure

| Path | Description |
|------|-------------|
| `PatientMonitorClient/` | Java **Swing** desktop client — doctor login/registration, patient management, live vital-sign simulation, waveform & digital-twin views, PDF/JSON export, i18n (EN/中文). |
| `PatientServer/` | Java **Servlet** backend — doctor authentication, patient CRUD, record persistence (PostgreSQL), packaged as a WAR (Tomcat 9 / webapp-runner). |

## Tech Stack

Gradle · Swing · Java Servlets · PostgreSQL (JDBC) · Gson · Jakarta Mail · JUnit 5.

## Build & Run

Each sub-project is built independently with Gradle:

```bash
# Backend (WAR)
cd PatientServer && ./gradlew build

# Desktop client
cd PatientMonitorClient && ./gradlew run
```

See `PatientMonitorClient/README.md` and `PatientServer/README.md` for detailed
setup, environment variables, and deployment notes.

## Status

- Authentication uses environment-variable based configuration (no hardcoded
  secrets). Password hashing is currently a placeholder and should be replaced
  with bcrypt/Argon2 before any production use.
- Email verification flow is stubbed and not yet enforced.

## License

[MIT](LICENSE) © 2026 JacksonZ8
