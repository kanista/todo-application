package com.example.todo_project.controller;

import com.example.todo_project.dto.*;
import com.example.todo_project.exception.ApplicationException;
import com.example.todo_project.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<CommonApiResponse<UserDTO>> registerUser(@RequestBody RegisterRequestDTO request) {
        logger.debug("Received registration request: {}", request);

        if (authService.checkEmailExists(request.getEmail())) {
            logger.warn("Registration failed: Email already exists: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new CommonApiResponse<>(HttpStatus.BAD_REQUEST.value(), "Email already exists", null));
        }

        try {
            UserDTO registeredUser = authService.registerUser(request);
            logger.info("User registered successfully: {}", registeredUser.getEmail());
            return ResponseEntity.ok(new CommonApiResponse<>(HttpStatus.OK.value(), "User registered successfully", registeredUser));
        } catch (Exception e) {
            logger.error("Error during registration: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error", null));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<CommonApiResponse<LoginResponseDTO>> loginUser(@RequestBody LoginRequestDTO request) {
        logger.debug("Received login request for email: {}", request.getEmail());

        try {
            LoginResponseDTO loginResponse = authService.loginUser(request.getEmail(), request.getPassword());
            logger.info("Login successful for email: {}", request.getEmail());
            return ResponseEntity.ok(new CommonApiResponse<>(HttpStatus.OK.value(), "Login successful", loginResponse));
        } catch (ApplicationException.InvalidCredentialsException e) {
            logger.warn("Invalid login attempt for email: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new CommonApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "Invalid email or password", null));
        } catch (ApplicationException.UserNotFoundException e) {
            logger.warn("Login failed: User not found for email: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new CommonApiResponse<>(HttpStatus.NOT_FOUND.value(), "User not found", null));
        } catch (Exception e) {
            logger.error("Exception during login: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error: " + e.getMessage(), null));
        }
    }

}