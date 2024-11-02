package com.example.todo_project.dto;

import com.example.todo_project.entity.Priority;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TodoResponseDTO {
    private Long id;
    private String title;
    private String description;
    private Priority priority;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;
    @JsonProperty("completed")
    private boolean completed;
    private UserDTO user;
}
