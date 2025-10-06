package com.todoapp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.todoapp.dto.AuthResponse;
import com.todoapp.dto.LoginRequest;
import com.todoapp.dto.RegisterRequest;
import com.todoapp.service.AuthService;
import com.todoapp.util.SecurityValidator;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * Authentication Controller with enhanced security validation
 * Implements comprehensive input validation and security measures
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private SecurityValidator securityValidator;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        
        // Enhanced security validation
        SecurityValidator.ValidationResult usernameValidation = 
            securityValidator.validateSecurely(request.getUsername(), SecurityValidator.ValidationType.USERNAME);
        if (!usernameValidation.isValid()) {
            return ResponseEntity.badRequest()
                .body(new AuthResponse(false, "Invalid username: " + usernameValidation.getMessage(), null, null));
        }

        SecurityValidator.ValidationResult emailValidation = 
            securityValidator.validateSecurely(request.getEmail(), SecurityValidator.ValidationType.EMAIL);
        if (!emailValidation.isValid()) {
            return ResponseEntity.badRequest()
                .body(new AuthResponse(false, "Invalid email: " + emailValidation.getMessage(), null, null));
        }

        SecurityValidator.ValidationResult passwordValidation = 
            securityValidator.validateSecurely(request.getPassword(), SecurityValidator.ValidationType.PASSWORD);
        if (!passwordValidation.isValid()) {
            return ResponseEntity.badRequest()
                .body(new AuthResponse(false, "Password must be 8-50 characters with uppercase, lowercase, digit, and special character", null, null));
        }

        // Validate password confirmation
        if (!request.isPasswordMatching()) {
            logSecurityEvent("REGISTER_PASSWORD_MISMATCH", request.getUsername(), httpRequest);
            return ResponseEntity.badRequest()
                .body(new AuthResponse(false, "Passwords do not match", null, null));
        }

        SecurityValidator.ValidationResult nameValidation = 
            securityValidator.validateSecurely(request.getName(), SecurityValidator.ValidationType.NAME);
        if (!nameValidation.isValid()) {
            return ResponseEntity.badRequest()
                .body(new AuthResponse(false, "Invalid name: " + nameValidation.getMessage(), null, null));
        }

        // Sanitize inputs
        request.setUsername(securityValidator.sanitizeInput(request.getUsername()));
        request.setEmail(securityValidator.sanitizeInput(request.getEmail()));
        request.setName(securityValidator.sanitizeInput(request.getName()));

        // Log security event
        logSecurityEvent("REGISTER_ATTEMPT", request.getUsername(), httpRequest);

        try {
            AuthResponse response = authService.register(request);
            if (response.isSuccess()) {
                logSecurityEvent("REGISTER_SUCCESS", request.getUsername(), httpRequest);
                return ResponseEntity.ok(response);
            } else {
                logSecurityEvent("REGISTER_FAILED", request.getUsername(), httpRequest);
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            logSecurityEvent("REGISTER_ERROR", request.getUsername(), httpRequest);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new AuthResponse(false, "Registration failed. Please try again.", null, null));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        
        // Enhanced security validation
        SecurityValidator.ValidationResult usernameValidation = 
            securityValidator.validateSecurely(request.getUsername(), SecurityValidator.ValidationType.USERNAME);
        if (!usernameValidation.isValid()) {
            logSecurityEvent("LOGIN_INVALID_USERNAME", request.getUsername(), httpRequest);
            return ResponseEntity.badRequest()
                .body(new AuthResponse(false, "Invalid username format", null, null));
        }

        // Basic password validation (don't reveal full requirements for login)
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            logSecurityEvent("LOGIN_EMPTY_PASSWORD", request.getUsername(), httpRequest);
            return ResponseEntity.badRequest()
                .body(new AuthResponse(false, "Password is required", null, null));
        }

        // Check for malicious content
        if (securityValidator.containsXSS(request.getUsername()) || 
            securityValidator.containsXSS(request.getPassword())) {
            logSecurityEvent("LOGIN_XSS_ATTEMPT", request.getUsername(), httpRequest);
            return ResponseEntity.badRequest()
                .body(new AuthResponse(false, "Invalid characters detected", null, null));
        }

        // Sanitize inputs
        request.setUsername(securityValidator.sanitizeInput(request.getUsername()));

        // Log security event
        logSecurityEvent("LOGIN_ATTEMPT", request.getUsername(), httpRequest);

        try {
            AuthResponse response = authService.login(request);
            if (response.isSuccess()) {
                logSecurityEvent("LOGIN_SUCCESS", request.getUsername(), httpRequest);
                return ResponseEntity.ok(response);
            } else {
                logSecurityEvent("LOGIN_FAILED", request.getUsername(), httpRequest);
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            logSecurityEvent("LOGIN_ERROR", request.getUsername(), httpRequest);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new AuthResponse(false, "Login failed. Please try again.", null, null));
        }
    }

    /**
     * Log security events for monitoring and alerting
     */
    private void logSecurityEvent(String event, String username, HttpServletRequest request) {
        String clientIP = getClientIP(request);
        String userAgent = request.getHeader("User-Agent");
        
        // In production, this should log to a security monitoring system
        System.out.printf("[SECURITY] %s - User: %s, IP: %s, UserAgent: %s%n", 
            event, username, clientIP, userAgent);
    }

    /**
     * Get real client IP address (considering proxies)
     */
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }
        return request.getRemoteAddr();
    }
}