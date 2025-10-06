package com.todoapp.config;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Simple Rate Limiting Configuration following Google security best practices
 * Implements different rate limits for different types of endpoints
 */
@Configuration
public class RateLimitingConfig {

    private final ConcurrentHashMap<String, RateLimitBucket> rateLimitBuckets = new ConcurrentHashMap<>();

    @Bean
    @Order(2)
    public Filter rateLimitingFilter() {
        return new RateLimitingFilter();
    }

    private static class RateLimitBucket {
        private int requests;
        private LocalDateTime windowStart;
        private final int maxRequests;
        private final long windowMinutes;

        public RateLimitBucket(int maxRequests, long windowMinutes) {
            this.maxRequests = maxRequests;
            this.windowMinutes = windowMinutes;
            this.windowStart = LocalDateTime.now();
            this.requests = 0;
        }

        public synchronized boolean tryConsume() {
            LocalDateTime now = LocalDateTime.now();
            
            // Reset window if expired
            if (ChronoUnit.MINUTES.between(windowStart, now) >= windowMinutes) {
                windowStart = now;
                requests = 0;
            }

            if (requests < maxRequests) {
                requests++;
                return true;
            }
            return false;
        }

        public int getRemainingRequests() {
            return Math.max(0, maxRequests - requests);
        }
    }

    private class RateLimitingFilter implements Filter {

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {

            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            String clientId = getClientIdentifier(httpRequest);
            String endpoint = httpRequest.getRequestURI();

            // Get appropriate bucket based on endpoint
            RateLimitBucket bucket = getBucketForEndpoint(clientId, endpoint);

            if (bucket.tryConsume()) {
                // Add rate limit headers
                addRateLimitHeaders(httpResponse, bucket);
                chain.doFilter(request, response);
            } else {
                // Rate limit exceeded
                httpResponse.setStatus(429); // Too Many Requests
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write(
                    "{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests. Please try again later.\"}"
                );
                addRateLimitHeaders(httpResponse, bucket);
            }
        }

        private String getClientIdentifier(HttpServletRequest request) {
            // Use X-Forwarded-For if behind proxy, otherwise use remote address
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }

        private RateLimitBucket getBucketForEndpoint(String clientId, String endpoint) {
            String bucketKey;
            int maxRequests;
            long windowMinutes;

            if (endpoint.startsWith("/api/auth/login")) {
                // Strict rate limiting for login attempts (5 attempts per minute)
                bucketKey = "login:" + clientId;
                maxRequests = 5;
                windowMinutes = 1;
            } else if (endpoint.startsWith("/api/auth/register")) {
                // Moderate rate limiting for registration (3 attempts per hour)
                bucketKey = "register:" + clientId;
                maxRequests = 3;
                windowMinutes = 60;
            } else if (endpoint.startsWith("/api/auth/")) {
                // General auth endpoints (10 per minute)
                bucketKey = "auth:" + clientId;
                maxRequests = 10;
                windowMinutes = 1;
            } else {
                // General API endpoints (100 per minute)
                bucketKey = "api:" + clientId;
                maxRequests = 100;
                windowMinutes = 1;
            }

            return rateLimitBuckets.computeIfAbsent(bucketKey, 
                key -> new RateLimitBucket(maxRequests, windowMinutes));
        }

        private void addRateLimitHeaders(HttpServletResponse response, RateLimitBucket bucket) {
            response.setHeader("X-Rate-Limit-Remaining", 
                String.valueOf(bucket.getRemainingRequests()));
            response.setHeader("X-Rate-Limit-Retry-After-Seconds", "60");
        }
    }
}