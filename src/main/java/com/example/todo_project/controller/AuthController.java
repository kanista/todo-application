package com.example.todo_project.controller;

import com.example.todo_project.dto.*;
import com.example.todo_project.exception.ApplicationException;
import com.example.todo_project.service.AuthService;
import com.example.todo_project.utils.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    public AuthController(AuthService authService, JwtUtil jwtUtil, AuthenticationManager authenticationManager) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/register")
    public ResponseEntity<CommonApiResponse<UserDTO>> registerUser(@RequestBody RegisterRequestDTO request) {
        logger.info("Received registration request: {}", request);

        if (authService.checkEmailExists(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new CommonApiResponse<>(HttpStatus.BAD_REQUEST.value(), "Email already exists", null));
        }

        try {
            UserDTO registeredUser = authService.registerUser(request);
            return ResponseEntity.ok(new CommonApiResponse<>(HttpStatus.OK.value(), "User registered successfully", registeredUser));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new CommonApiResponse<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error", null));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<CommonApiResponse<Map<String, String>>> loginUser(@RequestBody LoginRequestDTO request) {
        logger.info("Received login request for email: {}", request.getEmail());

        try {
            String jwt = authService.loginUser(request.getEmail(), request.getPassword());
            Map<String, String> responseData = new HashMap<>();
            responseData.put("token", jwt);
            return ResponseEntity.ok(new CommonApiResponse<>(HttpStatus.OK.value(), "Login successful", responseData));
        } catch (ApplicationException.InvalidCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new CommonApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "Invalid email or password", null));
        } catch (ApplicationException.UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new CommonApiResponse<>(HttpStatus.NOT_FOUND.value(), "User not found", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error: " + e.getMessage(), null));
        }
    }
}