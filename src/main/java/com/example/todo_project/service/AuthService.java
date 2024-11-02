package com.example.todo_project.service;

import com.example.todo_project.dto.RegisterRequestDTO;
import com.example.todo_project.dto.UserDTO;
import com.example.todo_project.entity.User;
import com.example.todo_project.exception.ApplicationException;
import com.example.todo_project.repository.UserRepository;
import com.example.todo_project.utils.JwtUtil;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
public class AuthService implements UserDetailsService {

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

        return userRepository.findByEmail(email).isPresent();
    }

    public UserDTO registerUser(RegisterRequestDTO request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new ApplicationException.PasswordMismatchException("Passwords do not match");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setRole(request.getRole());
        userRepository.save(user);
        return new UserDTO(user); // Convert to DTO
    }


    public String loginUser(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApplicationException.UserNotFoundException("User not found"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new ApplicationException.InvalidCredentialsException("Invalid credentials");
        }

        return jwtUtil.generateToken(user.getName(), user.getEmail(), user.getRole()); // Generate JWT token for the user
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Load user from database by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // Convert your User entity to UserDetails
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name()) // Assuming Role is an enum
                .build();
    }
}