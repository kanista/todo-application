package com.example.todo_project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommonApiResponse<T>{
    private int status;
    private String message;
    private T data;
}
