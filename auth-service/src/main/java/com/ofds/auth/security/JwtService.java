package com.ofds.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {
    private final Key key = Keys.hmacShaKeyFor("super-secret-key-change-me-please-super-secret".getBytes());

    public String extractUsername(String token) {
    	try {
    		return extractClaims(token).getSubject();
        } catch (Exception e) {
            System.out.println("Failed to extract username: " + e.getMessage());
            return null;
        }
    }

    public Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }

    public boolean isTokenValid(String token, String username) {
    	try {
            final String extractedUser = extractUsername(token);
            boolean valid = (extractedUser != null && extractedUser.equals(username) && !isTokenExpired(token));
            System.out.println("Token validation: extracted=" + extractedUser + " | expected=" + username + " | valid=" + valid);
            return valid;
        } catch (Exception e) {
            System.out.println("Token validation failed: " + e.getMessage());
            return false;
        }
    }

    public String generate(String subject, Map<String, Object> claims) {
    	long expirationMs = 1000 * 60 * 15; // 15 minutes
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }
}
