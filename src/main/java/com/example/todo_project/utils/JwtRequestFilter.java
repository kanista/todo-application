package com.example.todo_project.utils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class);

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public JwtRequestFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        logger.debug("Processing request for URI: {}", requestUri);

        String authorizationHeader = request.getHeader("Authorization");
        logger.debug("Authorization header received: {}", authorizationHeader);

        String username = null;
        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            logger.debug("Extracted JWT: {}", jwt);

            username = jwtUtil.extractUsername(jwt);
            logger.debug("Extracted username from JWT: {}", username);
        }

        // Ensure user is not authenticated already
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
            logger.debug("Loaded UserDetails for username: {}", username);

            // Validate the JWT
            if (jwtUtil.validateToken(jwt, userDetails.getUsername())) {
                logger.info("JWT validated successfully for user: {}", username);

                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                logger.debug("Set Authentication in SecurityContext for user: {}", username);
            } else {
                logger.warn("JWT validation failed for user: {}", username);
            }
        } else if (username != null) {
            logger.debug("User {} is already authenticated. Skipping re-authentication.", username);
        }

        // Continue with the filter chain
        chain.doFilter(request, response);
        logger.debug("Completed processing for URI: {}", requestUri);
    }
}
