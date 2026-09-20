package NetWork;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

// HTTP client for talking to backend API
public class ApiClient {

    private static final Gson gson = new Gson();
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 8000;

    private static ApiResult<String> postJsonResult(String endpoint, String jsonBody) {
        HttpURLConnection conn = null;
        try {
            conn = openJsonPost(endpoint, jsonBody);

            int code = conn.getResponseCode();
            String responseBody = readResponseBody(conn, code);
            if (code >= 200 && code < 300) {
                return ApiResult.ok(responseBody, code);
            }
            return ApiResult.error(extractMessage(responseBody, "Server returned HTTP " + code), code);
        } catch (IOException e) {
            return ApiResult.error("Cannot connect to server at " + ServerConfig.baseUrl() + ": " + e.getMessage(), -1);
        } catch (Exception e) {
            return ApiResult.error("Unexpected API error: " + e.getMessage(), -1);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static HttpURLConnection openJsonPost(String endpoint, String jsonBody) throws IOException {
        URL url = new URL(ServerConfig.url(endpoint));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setDoOutput(true);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }
        return conn;
    }

    private static String readResponseBody(HttpURLConnection conn, int code) throws IOException {
        InputStream stream = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        if (stream == null) return "";

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private static String extractMessage(String json, String fallback) {
        try {
            SimpleResponse response = gson.fromJson(json, SimpleResponse.class);
            if (response != null && response.message != null && !response.message.isBlank()) {
                return response.message;
            }
        } catch (Exception ignored) {
        }
        return json == null || json.isBlank() ? fallback : json;
    }

    private static <T> ApiResult<T> parse(ApiResult<String> raw, Class<T> type) {
        if (!raw.isSuccess()) {
            return ApiResult.error(raw.message, raw.statusCode);
        }

        try {
            return ApiResult.ok(gson.fromJson(raw.data, type), raw.statusCode);
        } catch (Exception e) {
            return ApiResult.error("Could not read server response: " + e.getMessage(), raw.statusCode);
        }
    }


    // Request models
    public static class LoginRequest {
        String email;
        String password;

        LoginRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }

    public static class RegisterRequest {
        String email;
        String password;
        String givenName;
        String familyName;

        RegisterRequest(String email, String password, String givenName, String familyName) {
            this.email = email;
            this.password = password;
            this.givenName = givenName;
            this.familyName = familyName;
        }
    }

    public static class DeleteAccountRequest {
        String email;
        String password;
        String confirm;

        DeleteAccountRequest(String email, String password, String confirm) {
            this.email = email;
            this.password = password;
            this.confirm = confirm;
        }
    }


    public static ApiResult<LoginResponse> loginResult(String email, String password) {
        LoginRequest req = new LoginRequest(email, password);
        ApiResult<LoginResponse> result = parse(postJsonResult("/login", gson.toJson(req)), LoginResponse.class);
        if (result.isSuccess() && result.data != null && !"ok".equals(result.data.status)) {
            return ApiResult.error(result.data.message, result.statusCode, result.data);
        }
        return result;
    }

    public static ApiResult<SimpleResponse> registerResult(String email, String password,
                                                           String givenName, String familyName) {
        RegisterRequest req = new RegisterRequest(email, password, givenName, familyName);
        ApiResult<SimpleResponse> result = parse(postJsonResult("/register", gson.toJson(req)), SimpleResponse.class);
        if (result.isSuccess() && result.data != null && !"ok".equals(result.data.status)) {
            return ApiResult.error(result.data.message, result.statusCode, result.data);
        }
        return result;
    }


    // delete account
    public static ApiResult<SimpleResponse> deleteAccountResult(String email, String password) {
        DeleteAccountRequest req = new DeleteAccountRequest(email, password, "DELETE");
        ApiResult<SimpleResponse> result = parse(postJsonResult("/deleteAccount", gson.toJson(req)), SimpleResponse.class);
        if (result.isSuccess() && result.data != null && !"ok".equals(result.data.status)) {
            return ApiResult.error(result.data.message, result.statusCode, result.data);
        }
        return result;
    }

    public static SimpleResponse deleteAccount(String email, String password) {
        ApiResult<SimpleResponse> result = deleteAccountResult(email, password);
        return result.isSuccess() ? result.data : null;
    }


    // Response models
    public static class LoginResponse {
        public String status;
        public String message;

        public Integer doctorId;
        public String givenName;
        public String familyName;
        public String token;
    }

    public static class SimpleResponse {
        public String status;
        public String message;
    }

    public static class ApiResult<T> {
        public final boolean success;
        public final T data;
        public final String message;
        public final int statusCode;

        private ApiResult(boolean success, T data, String message, int statusCode) {
            this.success = success;
            this.data = data;
            this.message = message;
            this.statusCode = statusCode;
        }

        public boolean isSuccess() {
            return success;
        }

        public static <T> ApiResult<T> ok(T data, int statusCode) {
            return new ApiResult<>(true, data, null, statusCode);
        }

        public static <T> ApiResult<T> error(String message, int statusCode) {
            return new ApiResult<>(false, null, message, statusCode);
        }

        public static <T> ApiResult<T> error(String message, int statusCode, T data) {
            return new ApiResult<>(false, data, message, statusCode);
        }
    }
}
