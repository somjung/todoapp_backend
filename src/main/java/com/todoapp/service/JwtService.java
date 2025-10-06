package com.todoapp.service;

import java.security.Key;
import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * Enhanced JWT Service with security best practices
 * Implements token blacklisting, secure token generation, and comprehensive validation
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refresh-token.expiration}")
    private long refreshExpiration;

    // Token blacklist for logout functionality
    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();
    
    // Secure random for additional entropy
    private final SecureRandom secureRandom = new SecureRandom();

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractTokenId(String token) {
        return extractClaim(token, Claims::getId);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        // Add security claims
        extraClaims.put("type", "access");
        extraClaims.put("iat", System.currentTimeMillis() / 1000);
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        claims.put("iat", System.currentTimeMillis() / 1000);
        return buildToken(claims, userDetails, refreshExpiration);
    }

    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration
    ) {
        // Generate unique token ID for tracking
        String tokenId = generateSecureTokenId();
        
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setId(tokenId)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .setIssuer("todoapp")
                .setAudience("todoapp-client")
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Enhanced token validation with blacklist checking
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            // Check if token is blacklisted
            if (isTokenBlacklisted(token)) {
                return false;
            }
            
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Validate refresh token
     */
    public boolean isRefreshTokenValid(String token, UserDetails userDetails) {
        try {
            if (isTokenBlacklisted(token)) {
                return false;
            }
            
            Claims claims = extractAllClaims(token);
            String tokenType = (String) claims.get("type");
            String username = claims.getSubject();
            
            return "refresh".equals(tokenType) && 
                   username.equals(userDetails.getUsername()) && 
                   !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Blacklist a token (for logout functionality)
     */
    public void blacklistToken(String token) {
        try {
            String tokenId = extractTokenId(token);
            if (tokenId != null) {
                blacklistedTokens.add(tokenId);
            }
        } catch (JwtException | IllegalArgumentException e) {
            // Token might be malformed, but we still want to blacklist it
            blacklistedTokens.add(token);
        }
    }

    /**
     * Check if token is blacklisted
     */
    private boolean isTokenBlacklisted(String token) {
        try {
            String tokenId = extractTokenId(token);
            return tokenId != null && blacklistedTokens.contains(tokenId);
        } catch (JwtException | IllegalArgumentException e) {
            return blacklistedTokens.contains(token);
        }
    }

    /**
     * Generate secure token ID with additional entropy
     */
    private String generateSecureTokenId() {
        byte[] randomBytes = new byte[16];
        secureRandom.nextBytes(randomBytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignInKey())
                .requireIssuer("todoapp")
                .requireAudience("todoapp-client")
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Clean up expired tokens from blacklist (should be called periodically)
     */
    public void cleanupBlacklist() {
        // This would typically be implemented with a scheduled task
        // For now, we'll keep it simple and rely on application restart
        // In production, consider using Redis or database for token blacklist
    }
}