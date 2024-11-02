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
        String username = extractClaim(token, Claims::getSubject);
        logger.debug("Extracted Username from token: {}", username);
        return username;
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY.getBytes())
                    .build()
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
            claims.put("role", role.toString());
            logger.debug("Generating JWT token for user: {}, role: {}", email, role);
            return createToken(claims, email);
        } catch (Exception e) {
            logger.error("Error generating JWT token: {}", e.getMessage());
            throw new RuntimeException("Token generation failed", e);
        }
    }


    private String createToken(Map<String, Object> claims, String subject) {
        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + JWT_EXPIRATION))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY.getBytes())
                .compact();
        logger.debug("JWT token created successfully for subject: {}", subject);
        return token;
    }

    public Boolean validateToken(String token, String username) {
        String extractedUsername = extractUsername(token);
        boolean isValid = extractedUsername.equals(username) && !isTokenExpired(token);
        logger.info("Token validation result - Username: {}, Valid: {}", extractedUsername, isValid);
        return isValid;
    }

    private Boolean isTokenExpired(String token) {
        boolean expired = extractExpiration(token).before(new Date());
        logger.debug("Token expiration status: {}", expired ? "Expired" : "Not expired");
        return expired;
    }

    private Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }
}
