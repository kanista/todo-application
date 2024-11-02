package com.example.todo_project.controller;

import com.example.todo_project.dto.CommonApiResponse;
import com.example.todo_project.dto.TodoResponseDTO;
import com.example.todo_project.entity.Todo;
import com.example.todo_project.exception.ApplicationException;
import com.example.todo_project.service.TodoService;
import com.example.todo_project.utils.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TodoController {

    private final TodoService todoService;
    private final JwtUtil jwtUtil;

    public TodoController(TodoService todoService, JwtUtil jwtUtil) {
        this.todoService = todoService;
        this.jwtUtil = jwtUtil;
    }

    private String validateTokenAndGetEmail(HttpServletRequest request) {
        String token = request.getHeader("Authorization");

        if (token == null || !token.startsWith("Bearer ")) {
            throw new JwtException("Invalid or missing token.");
        }

        String email = jwtUtil.extractUsername(token.substring(7));
        if (email == null) {
            throw new JwtException("Token validation failed, email not found.");
        }

        return email;
    }

    @PostMapping
    public ResponseEntity<CommonApiResponse<TodoResponseDTO>> createTask(@RequestBody Todo task, HttpServletRequest request) {

        try {
            String email = validateTokenAndGetEmail(request);
            TodoResponseDTO createdTask = todoService.createTask(task, email);
            CommonApiResponse<TodoResponseDTO> commonApiResponse = new CommonApiResponse<>(HttpStatus.CREATED.value(), "Task created successfully.", createdTask);
            return ResponseEntity.status(HttpStatus.CREATED).body(commonApiResponse);
        } catch (ApplicationException.UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new CommonApiResponse<>(HttpStatus.NOT_FOUND.value(), "User not found", null));
        } catch (ApplicationException.TaskAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new CommonApiResponse<>(HttpStatus.CONFLICT.value(), e.getMessage(), null));
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new CommonApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "Invalid token.", null));
        }  catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Task creation failed.", null));
        }
    }

    // Get all tasks for the authenticated user
    @GetMapping("/all-tasks")
    public ResponseEntity<CommonApiResponse<List<TodoResponseDTO>>> getAllTasks(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size) {

        try {
            String email = validateTokenAndGetEmail(request);
            Pageable pageable = PageRequest.of(page, size);

            // Fetch paginated tasks
            Page<TodoResponseDTO> tasks = todoService.getAllTasks(email, pageable);

            // Extract the content (list of tasks) from the Page object
            List<TodoResponseDTO> taskContent = tasks.getContent();

            // Wrap the task content list in a CommonApiResponse
            CommonApiResponse<List<TodoResponseDTO>> commonApiResponse = new CommonApiResponse<>(
                    HttpStatus.OK.value(),
                    "Tasks retrieved successfully.",
                    taskContent
            );

            return ResponseEntity.ok(commonApiResponse);
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new CommonApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "Invalid token.", null));
        } catch (Exception e) {
            CommonApiResponse<List<TodoResponseDTO>> commonApiResponse = new CommonApiResponse<>(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to retrieve tasks.",
                    null
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(commonApiResponse);
        }
    }

    // Get a task by ID
    @GetMapping("/{id}")
    public ResponseEntity<CommonApiResponse<TodoResponseDTO>> getTaskById(@PathVariable Long id, HttpServletRequest request) {


        try {
            String email = validateTokenAndGetEmail(request);
            TodoResponseDTO task = todoService.getTaskById(id, email);
            CommonApiResponse<TodoResponseDTO> commonApiResponse = new CommonApiResponse<>(HttpStatus.OK.value(), "Task retrieved successfully.", task);
            return ResponseEntity.ok(commonApiResponse);
        } catch (ApplicationException.TodoNotFoundException e) {
            CommonApiResponse<TodoResponseDTO> commonApiResponse = new CommonApiResponse<>(HttpStatus.NOT_FOUND.value(), "Task not available.", null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(commonApiResponse);
        } catch (UsernameNotFoundException e) {
            CommonApiResponse<TodoResponseDTO> commonApiResponse = new CommonApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "User not found.", null);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(commonApiResponse);
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new CommonApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "Invalid token.", null));
        } catch (Exception e) {
            CommonApiResponse<TodoResponseDTO> commonApiResponse = new CommonApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to retrieve the task.", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(commonApiResponse);
        }
    }

    // Update a task
    @PutMapping("/{id}")
    public ResponseEntity<CommonApiResponse<Todo>> updateTask(@PathVariable Long id, @RequestBody Todo task, HttpServletRequest request) {

        try {

            String email = validateTokenAndGetEmail(request);
            Todo updatedTask = todoService.updateTask(id, task, email);
            CommonApiResponse<Todo> commonApiResponse = new CommonApiResponse<>(HttpStatus.OK.value(), "Todo updated successfully.", updatedTask);
            return ResponseEntity.ok(commonApiResponse);
        } catch (ApplicationException.TodoNotFoundException e) {
            CommonApiResponse<Todo> commonApiResponse = new CommonApiResponse<>(HttpStatus.NOT_FOUND.value(), e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(commonApiResponse);
        } catch (ApplicationException.UnauthorizedAccessException e) {
            CommonApiResponse<Todo> commonApiResponse = new CommonApiResponse<>(HttpStatus.FORBIDDEN.value(), e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(commonApiResponse);
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new CommonApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "Invalid token.", null));
        } catch (Exception e) {
            CommonApiResponse<Todo> commonApiResponse = new CommonApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to update the todo.", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(commonApiResponse);
        }
    }


    // Delete a task
    @DeleteMapping("/{id}")
    public ResponseEntity<CommonApiResponse<Void>> deleteTask(@PathVariable Long id, HttpServletRequest request) {

        try {
            String email = validateTokenAndGetEmail(request);
            todoService.deleteTask(id, email);
            CommonApiResponse<Void> commonApiResponse = new CommonApiResponse<>(HttpStatus.OK.value(), "Todo deleted successfully.", null);
            return ResponseEntity.ok(commonApiResponse);
        } catch (ApplicationException.TodoNotFoundException e) {
            CommonApiResponse<Void> commonApiResponse = new CommonApiResponse<>(HttpStatus.NOT_FOUND.value(), e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(commonApiResponse);
        } catch (ApplicationException.UnauthorizedAccessException e) {
            // Corrected to match expected type CommonApiResponse<Void>
            CommonApiResponse<Void> commonApiResponse = new CommonApiResponse<>(HttpStatus.FORBIDDEN.value(), e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(commonApiResponse);
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new CommonApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "Invalid token.", null));
        } catch (Exception e) {
            CommonApiResponse<Void> commonApiResponse = new CommonApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to delete the todo.", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(commonApiResponse);
        }
    }

    // Get tasks by completion status
    @GetMapping("/completed")
    public ResponseEntity<CommonApiResponse<List<TodoResponseDTO>>> getTasksByCompletion(
            @RequestParam boolean completed, HttpServletRequest request) {

        try {
            String email = validateTokenAndGetEmail(request);
            List<TodoResponseDTO> taskResponseDtos = todoService.getTasksByCompletion(email, completed);
            CommonApiResponse<List<TodoResponseDTO>> commonApiResponse = new CommonApiResponse<>(HttpStatus.OK.value(), "Todos retrieved successfully.", taskResponseDtos);
            return ResponseEntity.ok(commonApiResponse);
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new CommonApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "Invalid token.", null));
        } catch (Exception e) {
            CommonApiResponse<List<TodoResponseDTO>> commonApiResponse = new CommonApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to retrieve Todos.", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(commonApiResponse);
        }
    }

}
