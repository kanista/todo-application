package com.example.todo_project.dto;

import com.example.todo_project.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequestDTO {

    private String email;

    private String password;

    private String confirmPassword;

    private String name;

    private Role role;

}
