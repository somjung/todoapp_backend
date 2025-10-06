package com.todoapp.util;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * Security validation utility following Google security best practices
 * Provides input validation and sanitization to prevent common web vulnerabilities
 */
@Component
public class SecurityValidator {

    // Regex patterns for validation
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,50}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z\\s]{2,50}$");
    private static final Pattern TITLE_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s\\-_.,!?]{1,100}$");
    private static final Pattern DESCRIPTION_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s\\-_.,!?\\n\\r]{0,500}$");
    
    // XSS patterns to block
    private static final Pattern[] XSS_PATTERNS = {
        Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onload=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onclick=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onerror=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<iframe[^>]*>.*?</iframe>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<object[^>]*>.*?</object>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<embed[^>]*>.*?</embed>", Pattern.CASE_INSENSITIVE)
    };

    /**
     * Validate username format
     */
    public boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }

    /**
     * Validate email format
     */
    public boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Validate password strength
     * Requirements: 8-50 chars, at least one uppercase, lowercase, digit, and special character
     */
    public boolean isValidPassword(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * Validate name format
     */
    public boolean isValidName(String name) {
        return name != null && NAME_PATTERN.matcher(name).matches();
    }

    /**
     * Validate title format (for todos and collections)
     */
    public boolean isValidTitle(String title) {
        return title != null && TITLE_PATTERN.matcher(title).matches();
    }

    /**
     * Validate description format
     */
    public boolean isValidDescription(String description) {
        return description == null || DESCRIPTION_PATTERN.matcher(description).matches();
    }

    /**
     * Sanitize input by removing potentially dangerous content
     */
    public String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }

        String sanitized = input.trim();

        // Remove potential XSS content
        for (Pattern pattern : XSS_PATTERNS) {
            sanitized = pattern.matcher(sanitized).replaceAll("");
        }

        // Remove HTML tags
        sanitized = sanitized.replaceAll("<[^>]*>", "");

        // Encode special characters
        sanitized = sanitized
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("/", "&#x2F;");

        return sanitized;
    }

    /**
     * Check for SQL injection patterns
     */
    public boolean containsSqlInjection(String input) {
        if (input == null) {
            return false;
        }

        String lowerInput = input.toLowerCase();
        String[] sqlKeywords = {
            "union", "select", "insert", "update", "delete", "drop", "create", "alter",
            "exec", "execute", "sp_", "xp_", "'-", "\"", ";", "--", "/*", "*/"
        };

        for (String keyword : sqlKeywords) {
            if (lowerInput.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validate that input doesn't contain XSS attempts
     */
    public boolean containsXSS(String input) {
        if (input == null) {
            return false;
        }

        for (Pattern pattern : XSS_PATTERNS) {
            if (pattern.matcher(input).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Comprehensive security validation
     */
    public ValidationResult validateSecurely(String input, ValidationType type) {
        if (input == null) {
            return new ValidationResult(false, "Input cannot be null");
        }

        // Check for XSS
        if (containsXSS(input)) {
            return new ValidationResult(false, "Input contains potentially malicious content");
        }

        // Check for SQL injection
        if (containsSqlInjection(input)) {
            return new ValidationResult(false, "Input contains potentially dangerous SQL content");
        }

        // Type-specific validation
        boolean isValid = switch (type) {
            case USERNAME -> isValidUsername(input);
            case EMAIL -> isValidEmail(input);
            case PASSWORD -> isValidPassword(input);
            case NAME -> isValidName(input);
            case TITLE -> isValidTitle(input);
            case DESCRIPTION -> isValidDescription(input);
        };

        if (!isValid) {
            return new ValidationResult(false, "Input format is invalid for " + type);
        }

        return new ValidationResult(true, "Valid");
    }

    public enum ValidationType {
        USERNAME, EMAIL, PASSWORD, NAME, TITLE, DESCRIPTION
    }

    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
    }
}