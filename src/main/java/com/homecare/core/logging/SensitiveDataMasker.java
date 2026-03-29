package com.homecare.core.logging;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Utility to redact sensitive data (passwords, tokens, card numbers, etc.)
 * from log output. Used by {@link LoggingAspect} and {@link com.homecare.core.filter.RequestLoggingFilter}.
 */
public final class SensitiveDataMasker {

    private SensitiveDataMasker() {}

    // ─── Field names that should always be fully masked ──────────────
    private static final Set<String> FULLY_MASKED_FIELDS = Set.of(
            "password", "passwordHash", "newPassword", "oldPassword",
            "currentPassword", "confirmPassword",
            "accessToken", "refreshToken", "token", "tokenHash",
            "authorization", "secret", "apiKey",
            "razorpaySignature", "razorpayPaymentId",
            "otp", "pin", "cvv", "cardNumber"
    );

    // ─── Regex patterns for inline value masking ─────────────────────

    /** Bearer tokens in header-like strings */
    private static final Pattern BEARER_PATTERN =
            Pattern.compile("(Bearer\\s+)[A-Za-z0-9\\-_.]+", Pattern.CASE_INSENSITIVE);

    /** Credit/debit card numbers (13–19 digits, possibly grouped) */
    private static final Pattern CARD_PATTERN =
            Pattern.compile("\\b(\\d{4})[- ]?\\d{4,}[- ]?\\d{4,}[- ]?(\\d{4})\\b");

    /** Email addresses — keep first 2 chars + domain */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("\\b([A-Za-z0-9._%+-]{2})[A-Za-z0-9._%+-]*@([A-Za-z0-9.-]+\\.[A-Z|a-z]{2,})\\b");

    // ─── Public API ──────────────────────────────────────────────────

    /**
     * Returns true if a field/parameter name should be completely masked.
     */
    public static boolean isSensitiveField(String fieldName) {
        if (fieldName == null) return false;
        return FULLY_MASKED_FIELDS.contains(fieldName)
                || FULLY_MASKED_FIELDS.stream().anyMatch(f -> fieldName.equalsIgnoreCase(f));
    }

    /**
     * Masks a value completely (e.g., for password fields).
     */
    public static String mask(String value) {
        if (value == null || value.isEmpty()) return value;
        return "***";
    }

    /**
     * Masks sensitive patterns found inline in arbitrary text
     * (bearer tokens, card numbers). Suitable for request/response body logging.
     */
    public static String maskInline(String text) {
        if (text == null || text.isEmpty()) return text;

        String result = text;

        // Mask bearer tokens
        result = BEARER_PATTERN.matcher(result).replaceAll("$1***");

        // Mask card numbers → keep first 4 + last 4
        result = CARD_PATTERN.matcher(result).replaceAll("$1-****-****-$2");

        // Mask password-like JSON fields: "password":"value" → "password":"***"
        result = result.replaceAll(
                "(?i)(\"(?:password|token|secret|apiKey|otp|pin|cvv|refreshToken|accessToken|razorpaySignature)\"\\s*:\\s*)\"[^\"]*\"",
                "$1\"***\"");

        return result;
    }

    /**
     * Returns a masked version of a parameter value if the parameter name
     * is sensitive; otherwise returns the original value.
     */
    public static String maskIfSensitive(String paramName, Object value) {
        if (isSensitiveField(paramName)) {
            return "***";
        }
        return value != null ? value.toString() : "null";
    }
}

