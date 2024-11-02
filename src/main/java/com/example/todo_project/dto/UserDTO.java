package com.example.todo_project.dto;

import com.example.todo_project.entity.Role;
import com.example.todo_project.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class UserDTO {
    private Long id;
    private String name;
    private String email;
    private Role role;

    // Constructor to convert User entity to UserDto
    public UserDTO(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.role = user.getRole();
    }

}
