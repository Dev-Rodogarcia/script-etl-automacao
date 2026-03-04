package br.com.extrator.util.log;

import java.util.regex.Pattern;

/**
 * Sanitizes sensitive fragments before logging or persisting operational errors.
 */
public final class SensitiveDataSanitizer {

    private static final String MASK = "***";

    private static final Pattern BEARER_PATTERN =
        Pattern.compile("(?i)\\bBearer\\s+([A-Za-z0-9\\-._~+/]+=*)");
    private static final Pattern KEY_VALUE_PATTERN =
        Pattern.compile("(?i)\\b(password|senha|token|api[_-]?key|secret|client[_-]?secret|authorization)\\b\\s*[:=]\\s*([^,;\\s]+)");
    private static final Pattern JSON_KEY_PATTERN =
        Pattern.compile("(?i)\"(password|senha|token|api[_-]?key|secret|client[_-]?secret|authorization)\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern JDBC_PASSWORD_PATTERN =
        Pattern.compile("(?i)(password\\s*=\\s*)([^;\\s]+)");
    private static final Pattern JDBC_USER_PATTERN =
        Pattern.compile("(?i)(user(?:name|id)?\\s*=\\s*)([^;\\s]+)");

    private SensitiveDataSanitizer() {
        // utility
    }

    public static String sanitize(final String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        String sanitized = value;
        sanitized = BEARER_PATTERN.matcher(sanitized).replaceAll("Bearer " + MASK);
        sanitized = KEY_VALUE_PATTERN.matcher(sanitized).replaceAll("$1=" + MASK);
        sanitized = JSON_KEY_PATTERN.matcher(sanitized).replaceAll("\"$1\":\"" + MASK + "\"");
        sanitized = JDBC_PASSWORD_PATTERN.matcher(sanitized).replaceAll("$1" + MASK);
        sanitized = JDBC_USER_PATTERN.matcher(sanitized).replaceAll("$1" + MASK);
        return sanitized;
    }
}
