package com.example.todo_project.service;

import com.example.todo_project.dto.LoginResponseDTO;
import com.example.todo_project.dto.RegisterRequestDTO;
import com.example.todo_project.dto.UserDTO;
import com.example.todo_project.entity.User;
import com.example.todo_project.exception.ApplicationException;
import com.example.todo_project.repository.UserRepository;
import com.example.todo_project.utils.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
public class AuthService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // Constructor injection
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    // Method to check if email already exists
    public boolean checkEmailExists(String email) {
        boolean exists = userRepository.findByEmail(email).isPresent();
        logger.info("Checked if email exists ({}): {}", email, exists);
        return exists;
    }

    public UserDTO registerUser(RegisterRequestDTO request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            logger.error("Password mismatch for email: {}", request.getEmail());
            throw new ApplicationException.PasswordMismatchException("Passwords do not match");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setRole(request.getRole());

        userRepository.save(user);
        logger.info("User registered successfully: {}", request.getEmail());

        return new UserDTO(user);
    }


    public LoginResponseDTO loginUser(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.error("User not found for email: {}", email);
                    return new ApplicationException.UserNotFoundException("User not found");
                });
        if (!passwordEncoder.matches(password, user.getPassword())) {
            logger.error("Invalid credentials for email: {}", email);
            throw new ApplicationException.InvalidCredentialsException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(user.getName(), user.getEmail(), user.getRole());
        logger.info("User logged in successfully: {}", email);
        return new LoginResponseDTO(token, user.getName(), user.getEmail());
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.error("User not found with email: {}", email);
                    return new UsernameNotFoundException("User not found with email: " + email);
                });

        // Convert your User entity to UserDetails
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();
    }
}