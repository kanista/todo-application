package com.example.todo_project;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.example.todo_project.controller.AuthController;
import com.example.todo_project.dto.LoginRequestDTO;
import com.example.todo_project.dto.RegisterRequestDTO;
import com.example.todo_project.dto.UserDTO;
import com.example.todo_project.entity.Role;
import com.example.todo_project.exception.ApplicationException;
import com.example.todo_project.service.AuthService;
import com.example.todo_project.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;


@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    public void testRegisterUser_Success() throws Exception {
        RegisterRequestDTO registerRequest = new RegisterRequestDTO(
                "test@example.com",
                "password123",
                "password123",
                "John Doe",
                Role.USER
        );

        UserDTO userDTO = new UserDTO(1L, "John Doe", "test@example.com", Role.USER);

        when(authService.checkEmailExists(registerRequest.getEmail())).thenReturn(false);
        when(authService.registerUser(registerRequest)).thenReturn(userDTO);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"test@example.com\", \"password\": \"password123\", \"confirmPassword\": \"password123\", \"name\": \"John Doe\", \"role\": \"USER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data.name").value("John Doe")) // Adjusted to match "name" field
                .andExpect(jsonPath("$.data.email").value("test@example.com")) // Check email instead of username
                .andExpect(jsonPath("$.data.role").value("USER"));
    }


    @Test
    public void testRegisterUser_EmailAlreadyExists() throws Exception {
        RegisterRequestDTO registerRequest = new RegisterRequestDTO(
                "test@example.com",
                "password123",
                "password123",
                "John Doe",
                Role.USER
        );

        when(authService.checkEmailExists(registerRequest.getEmail())).thenReturn(true);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"testuser\", \"email\": \"test@example.com\", \"password\": \"password123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }


    @Test
    public void testLoginUser_Success() throws Exception {
        LoginRequestDTO loginRequest = new LoginRequestDTO("test@example.com", "password123");
        String token = "mocked-jwt-token";

        when(authService.loginUser(loginRequest.getEmail(), loginRequest.getPassword())).thenReturn(token);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"test@example.com\", \"password\": \"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.token").value("mocked-jwt-token"));
    }

    @Test
    public void testLoginUser_InvalidCredentials() throws Exception {
        LoginRequestDTO loginRequest = new LoginRequestDTO("test@example.com", "wrongpassword");

        when(authService.loginUser(loginRequest.getEmail(), loginRequest.getPassword()))
                .thenThrow(new ApplicationException.InvalidCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"test@example.com\", \"password\": \"wrongpassword\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(HttpStatus.UNAUTHORIZED.value()))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    public void testLoginUser_UserNotFound() throws Exception {
        LoginRequestDTO loginRequest = new LoginRequestDTO("unknown@example.com", "password123");

        when(authService.loginUser(loginRequest.getEmail(), loginRequest.getPassword()))
                .thenThrow(new ApplicationException.UserNotFoundException("User not found"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"unknown@example.com\", \"password\": \"password123\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    public void testLoginUser_InternalServerError() throws Exception {
        LoginRequestDTO loginRequest = new LoginRequestDTO("test@example.com", "password123");

        when(authService.loginUser(loginRequest.getEmail(), loginRequest.getPassword()))
                .thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"test@example.com\", \"password\": \"password123\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(HttpStatus.INTERNAL_SERVER_ERROR.value()))
                .andExpect(jsonPath("$.message").value("Internal Server Error: Unexpected error"));
    }
}
