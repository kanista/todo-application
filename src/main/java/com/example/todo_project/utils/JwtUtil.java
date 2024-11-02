package com.example.todo_project.utils;

import com.example.todo_project.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    private final String SECRET_KEY = "your-secure-predefined-new-key-here";
    private final long JWT_EXPIRATION = 86400000;

    public String extractUsername(String token) {
        logger.info("Extracted Username: {}", extractClaim(token, Claims::getSubject)); // Should print the email
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(SECRET_KEY.getBytes()) // Convert the key to bytes
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
            throw new JwtException("Invalid JWT token: " + e.getMessage(), e);
        }
    }


    public String generateToken(String name, String email, Role role) {
        try {
            Map<String, Object> claims = new HashMap<>();
            claims.put("name", name);
            claims.put("role", role); // Ensure role is a string
            return createToken(claims, email);
        } catch (Exception e) {
            e.printStackTrace(); // Log the error for debugging
            logger.error("Error generating JWT token: {}", e.getMessage());
            throw new RuntimeException("Token generation failed: " + e.getMessage());
        }
    }


    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(new Date().getTime() + JWT_EXPIRATION))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY.getBytes())
                .compact();
    }

    public Boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        Boolean tokenValid = extractedUsername.equals(username) && !isTokenExpired(token);
        logger.info("Token Validation - Extracted Username: {}", extractedUsername);
        logger.info("Token Validity: {}", tokenValid);
        return tokenValid;
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }
}
