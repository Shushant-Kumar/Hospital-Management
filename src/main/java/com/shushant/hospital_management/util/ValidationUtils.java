package com.shushant.hospital_management.util;

import java.sql.Date;
import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Centralized input validation for all DAO and UI operations.
 * Methods throw IllegalArgumentException with descriptive messages on failure.
 */
public final class ValidationUtils {

    private ValidationUtils() {}

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[6-9]\\d{9}$"); // Indian 10-digit mobile

    // ── String Validation ───────────────────────────────────────────────────

    public static String requireNonEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required and cannot be empty.");
        }
        return value.trim();
    }

    public static String requireMaxLength(String value, int maxLen, String fieldName) {
        if (value != null && value.length() > maxLen) {
            throw new IllegalArgumentException(fieldName + " exceeds maximum length of " + maxLen + " characters.");
        }
        return value;
    }

    // ── Numeric Validation ──────────────────────────────────────────────────

    public static double requireNonNegative(double value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative. Got: " + value);
        }
        return value;
    }

    public static double requirePositive(double value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive. Got: " + value);
        }
        return value;
    }

    public static int requirePositiveInt(int value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be a positive integer. Got: " + value);
        }
        return value;
    }

    public static int requireNonNegativeInt(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative. Got: " + value);
        }
        return value;
    }

    // ── Format Validation ───────────────────────────────────────────────────

    public static String requireValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) return email; // allow optional
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }
        return email.trim();
    }

    public static String requireValidPhone(String phone, String fieldName) {
        String cleaned = requireNonEmpty(phone, fieldName).replaceAll("[\\s-]", "");
        if (!PHONE_PATTERN.matcher(cleaned).matches()) {
            throw new IllegalArgumentException(fieldName + " must be a valid 10-digit Indian mobile number. Got: " + phone);
        }
        return cleaned;
    }

    // ── Date Validation ─────────────────────────────────────────────────────

    public static Date requireValidDate(String dateStr, String fieldName) {
        requireNonEmpty(dateStr, fieldName);
        try {
            return Date.valueOf(dateStr.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(fieldName + " must be in YYYY-MM-DD format. Got: " + dateStr);
        }
    }

    public static Date requireFutureOrToday(Date date, String fieldName) {
        if (date == null) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        if (date.toLocalDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(fieldName + " cannot be in the past. Got: " + date);
        }
        return date;
    }

    public static Date requirePastOrToday(Date date, String fieldName) {
        if (date == null) return null; // allow optional
        if (date.toLocalDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(fieldName + " cannot be in the future. Got: " + date);
        }
        return date;
    }

    // ── FK Existence Validation ─────────────────────────────────────────────

    public static void requireNotNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required and cannot be null.");
        }
    }
}
