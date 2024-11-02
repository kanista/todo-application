package com.example.todo_project.controller;

import com.example.todo_project.dto.CommonApiResponse;
import com.example.todo_project.dto.TodoResponseDTO;
import com.example.todo_project.entity.Priority;
import com.example.todo_project.entity.Todo;
import com.example.todo_project.exception.ApplicationException;
import com.example.todo_project.service.TodoService;
import com.example.todo_project.utils.JwtUtil;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TodoController {

    private final TodoService todoService;
    private final JwtUtil jwtUtil;
    private static final Logger logger = LoggerFactory.getLogger(TodoController.class);

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
        logger.debug("Received request to create task: {}", task);

        try {
            String email = validateTokenAndGetEmail(request);
            TodoResponseDTO createdTask = todoService.createTask(task, email);
            logger.info("Task created successfully for user: {}", email);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new CommonApiResponse<>(HttpStatus.CREATED.value(), "Task created successfully.", createdTask));
        } catch (ApplicationException.UserNotFoundException e) {
            logger.warn("User not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new CommonApiResponse<>(HttpStatus.NOT_FOUND.value(), "User not found", null));
        } catch (ApplicationException.TaskAlreadyExistsException e) {
            logger.warn("Task already exists: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new CommonApiResponse<>(HttpStatus.CONFLICT.value(), e.getMessage(), null));
        } catch (ApplicationException.JwtException e) {
            logger.error("JWT error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new CommonApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "Invalid token.", null));
        } catch (Exception e) {
            logger.error("Error creating task: {}", e.getMessage(), e);
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

        logger.debug("Received request to get all tasks for page: {}, size: {}", page, size);

        try {
            String email = validateTokenAndGetEmail(request);
            Pageable pageable = PageRequest.of(page, size);
            Page<TodoResponseDTO> tasks = todoService.getAllTasks(email, pageable);
            logger.info("Tasks retrieved successfully for user: {}", email);

            return ResponseEntity.ok(new CommonApiResponse<>(HttpStatus.OK.value(), "Tasks retrieved successfully.", tasks.getContent()));
        } catch (ApplicationException.JwtException e) {
            logger.error("JWT error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new CommonApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "Invalid token.", null));
        } catch (Exception e) {
            logger.error("Error retrieving tasks: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to retrieve tasks.", null));
        }
    }


    // Get a task by ID
    @GetMapping("/{id}")
    public ResponseEntity<CommonApiResponse<TodoResponseDTO>> getTaskById(@PathVariable Long id, HttpServletRequest request) {
        logger.debug("Received request to get task by ID: {}", id);

        try {
            String email = validateTokenAndGetEmail(request);
            TodoResponseDTO task = todoService.getTaskById(id, email);
            logger.info("Task retrieved successfully for user: {}, task ID: {}", email, id);
            return ResponseEntity.ok(new CommonApiResponse<>(HttpStatus.OK.value(), "Task retrieved successfully.", task));
        } catch (ApplicationException.TodoNotFoundException e) {
            logger.warn("Task not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new CommonApiResponse<>(HttpStatus.NOT_FOUND.value(), "Task not available.", null));
        } catch (ApplicationException.JwtException e) {
            logger.error("JWT error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new CommonApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "Invalid token.", null));
        } catch (Exception e) {
            logger.error("Error retrieving task by ID: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to retrieve the task.", null));
        }
    }

    // Update a task
    @PutMapping("/{id}")
    public ResponseEntity<CommonApiResponse<Todo>> updateTask(@PathVariable Long id, @RequestBody Todo task, HttpServletRequest request) {
        logger.debug("Received request to update task ID: {}", id);

        try {
            String email = validateTokenAndGetEmail(request);
            Todo updatedTask = todoService.updateTask(id, task, email);
            logger.info("Task updated successfully for user: {}, task ID: {}", email, id);
            return ResponseEntity.ok(new CommonApiResponse<>(HttpStatus.OK.value(), "Todo updated successfully.", updatedTask));
        } catch (ApplicationException.TodoNotFoundException e) {
            logger.warn("Task not found for update: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new CommonApiResponse<>(HttpStatus.NOT_FOUND.value(), e.getMessage(), null));
        } catch (ApplicationException.UnauthorizedAccessException e) {
            logger.warn("Unauthorized access attempt: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new CommonApiResponse<>(HttpStatus.FORBIDDEN.value(), e.getMessage(), null));
        } catch (ApplicationException.JwtException e) {
            logger.error("JWT error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new CommonApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "Invalid token.", null));
        } catch (Exception e) {
            logger.error("Error updating task: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to update the todo.", null));
        }
    }

    // Delete a task
    @DeleteMapping("/{id}")
    public ResponseEntity<CommonApiResponse<Void>> deleteTask(@PathVariable Long id, HttpServletRequest request) {
        logger.debug("Received request to delete task ID: {}", id);

        try {
            String email = validateTokenAndGetEmail(request);
            todoService.deleteTask(id, email);
            logger.info("Task deleted successfully for user: {}, task ID: {}", email, id);
            return ResponseEntity.ok(new CommonApiResponse<>(HttpStatus.OK.value(), "Todo deleted successfully.", null));
        } catch (ApplicationException.TodoNotFoundException e) {
            logger.warn("Task not found for deletion: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new CommonApiResponse<>(HttpStatus.NOT_FOUND.value(), e.getMessage(), null));
        } catch (ApplicationException.UnauthorizedAccessException e) {
            logger.warn("Unauthorized access attempt for deletion: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new CommonApiResponse<>(HttpStatus.FORBIDDEN.value(), e.getMessage(), null));
        } catch (ApplicationException.JwtException e) {
            logger.error("JWT error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new CommonApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "Invalid token.", null));
        } catch (Exception e) {
            logger.error("Error deleting task: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to delete the todo.", null));
        }
    }

    // Get tasks by completion status
    @GetMapping("/completed")
    public ResponseEntity<CommonApiResponse<List<TodoResponseDTO>>> getTasksByCompletion(
            @RequestParam boolean completed, HttpServletRequest request) {

        logger.debug("Received request to get tasks by completion status: {}", completed);

        try {
            String email = validateTokenAndGetEmail(request);
            List<TodoResponseDTO> taskResponseDtos = todoService.getTasksByCompletion(email, completed);
            logger.info("Tasks retrieved by completion status for user: {}", email);
            return ResponseEntity.ok(new CommonApiResponse<>(HttpStatus.OK.value(), "Todos retrieved successfully.", taskResponseDtos));
        } catch (ApplicationException.JwtException e) {
            logger.error("JWT error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new CommonApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "Invalid token.", null));
        } catch (Exception e) {
            logger.error("Error retrieving tasks by completion status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to retrieve todos by completion status.", null));
        }
    }

    // Get tasks by priority
    @GetMapping("/by-priority")
    public ResponseEntity<CommonApiResponse<List<TodoResponseDTO>>> getTasksByPriority(
            @RequestParam Priority priority,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size,
            HttpServletRequest request) {

        logger.debug("Received request to get tasks by priority: {}, page: {}, size: {}", priority, page, size);

        try {
            String email = validateTokenAndGetEmail(request);
            Pageable pageable = PageRequest.of(page, size);
            Page<TodoResponseDTO> taskResponseDtos = todoService.getTasksByPriority(email, priority, pageable);

            List<TodoResponseDTO> taskContent = taskResponseDtos.getContent();

            CommonApiResponse<List<TodoResponseDTO>> commonApiResponse = new CommonApiResponse<>(
                    HttpStatus.OK.value(),
                    "Todos retrieved successfully.",
                    taskContent
            );

            logger.info("Successfully retrieved tasks by priority for user: {}", email);
            return ResponseEntity.ok(commonApiResponse);
        } catch (ApplicationException.JwtException e) {
            logger.error("JWT error while retrieving tasks by priority: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new CommonApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "Invalid token.", null));
        } catch (Exception e) {
            logger.error("Error retrieving tasks by priority: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to retrieve Todos.", null));
        }
    }

    // Get tasks by title
    @GetMapping("/search-by-title")
    public ResponseEntity<CommonApiResponse<List<TodoResponseDTO>>> searchTasksByTitle(
            @RequestParam String title,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size,
            HttpServletRequest request) {

        logger.debug("Received request to search tasks by title: {}, page: {}, size: {}", title, page, size);

        try {
            String email = validateTokenAndGetEmail(request);
            Pageable pageable = PageRequest.of(page, size);
            Page<TodoResponseDTO> taskResponseDtos = todoService.searchTasksByTitle(email, title, pageable);

            List<TodoResponseDTO> taskContent = taskResponseDtos.getContent();

            if (taskContent.isEmpty()) {
                logger.info("No tasks found for title: {} for user: {}", title, email);
                return ResponseEntity.ok(new CommonApiResponse<>(HttpStatus.OK.value(), "No tasks found matching the title.", Collections.emptyList()));
            }

            CommonApiResponse<List<TodoResponseDTO>> commonApiResponse = new CommonApiResponse<>(
                    HttpStatus.OK.value(),
                    "Todos retrieved successfully.",
                    taskContent
            );

            logger.info("Successfully retrieved tasks by title for user: {}", email);
            return ResponseEntity.ok(commonApiResponse);
        } catch (ApplicationException.JwtException e) {
            logger.error("JWT error while searching tasks by title: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new CommonApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "Invalid token.", null));
        } catch (Exception e) {
            logger.error("Error searching tasks by title: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to retrieve Todos.", null));
        }
    }

}
