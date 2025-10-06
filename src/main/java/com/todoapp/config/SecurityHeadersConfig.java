package com.todoapp.config;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Security Headers Configuration following Google security best practices
 * Implements comprehensive security headers to protect against common web vulnerabilities
 */
@Configuration
public class SecurityHeadersConfig {

    /**
     * Custom security headers filter that adds additional security headers
     * beyond what Spring Security provides by default
     */
    @Bean
    @Order(1)
    public Filter securityHeadersFilter() {
        return new SecurityHeadersFilter();
    }

    private static class SecurityHeadersFilter implements Filter {
        
        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            
            // Content Security Policy (CSP) - Prevents XSS attacks
            httpResponse.setHeader("Content-Security-Policy",
                "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                "font-src 'self' https://fonts.gstatic.com; " +
                "img-src 'self' data: https:; " +
                "connect-src 'self' http://localhost:8080 https:; " +
                "frame-ancestors 'none'; " +
                "base-uri 'self'; " +
                "form-action 'self'"
            );
            
            // X-Content-Type-Options - Prevents MIME sniffing
            httpResponse.setHeader("X-Content-Type-Options", "nosniff");
            
            // X-Frame-Options - Prevents clickjacking (backup for frame-ancestors)
            httpResponse.setHeader("X-Frame-Options", "DENY");
            
            // X-XSS-Protection - Legacy XSS protection
            httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
            
            // Referrer Policy - Controls referrer information
            httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            
            // Permissions Policy - Controls browser feature access
            httpResponse.setHeader("Permissions-Policy",
                "camera=(), microphone=(), geolocation=(), payment=(), usb=(), " +
                "fullscreen=(self), accelerometer=(), gyroscope=(), magnetometer=()"
            );
            
            // Clear Server header to avoid information disclosure
            httpResponse.setHeader("Server", "");
            
            // Cache Control for sensitive endpoints
            String requestURI = ((jakarta.servlet.http.HttpServletRequest) request).getRequestURI();
            if (requestURI.contains("/api/auth/") || requestURI.contains("/api/users/")) {
                httpResponse.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, private");
                httpResponse.setHeader("Pragma", "no-cache");
                httpResponse.setHeader("Expires", "0");
            }
            
            chain.doFilter(request, response);
        }
    }
}