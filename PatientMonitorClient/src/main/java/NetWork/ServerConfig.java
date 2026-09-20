package NetWork;

// Central place for server URLs
public final class ServerConfig {
    private ServerConfig() {}

    // Fallback server address: keep the existing Tsuru backend path for now.
    private static final String DEFAULT_BASE = "https://bioeng-bbb-app.impaas.uk";

    // Decide which server base URL to use
    public static String baseUrl() {
        // 1) JVM argument. patient.server.url is used by the client preview plan;
        // server.base is kept for backwards compatibility with this repo.
        String jvm = firstPresent(
                System.getProperty("patient.server.url"),
                System.getProperty("server.base")
        );
        if (jvm != null && !jvm.isBlank()) return trimSlash(jvm);

        // 2) Environment variable. PATIENT_SERVER_BASE is kept for compatibility.
        String env = firstPresent(
                System.getenv("PATIENT_SERVER_URL"),
                System.getenv("PATIENT_SERVER_BASE")
        );
        if (env != null && !env.isBlank()) return trimSlash(env);

        // 3) Default
        return trimSlash(DEFAULT_BASE);
    }

    // Build full API URL
    public static String url(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) return baseUrl();
        if (!endpoint.startsWith("/")) endpoint = "/" + endpoint;
        return baseUrl() + endpoint;
    }

    // Remove trailing slashes
    private static String trimSlash(String s) {
        s = s.trim();
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s;
    }

    private static String firstPresent(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) return primary;
        return fallback;
    }
}
