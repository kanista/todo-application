package com.example.todo_project.service;

import com.example.todo_project.dto.TodoResponseDTO;
import com.example.todo_project.dto.UserDTO;
import com.example.todo_project.entity.Priority;
import com.example.todo_project.entity.Todo;
import com.example.todo_project.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.todo_project.exception.ApplicationException;
import com.example.todo_project.repository.TodoRepository;
import com.example.todo_project.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TodoService {

    private final TodoRepository todoRepository;
    private final UserRepository userRepository;
    private static final Logger logger = LoggerFactory.getLogger(TodoService.class);

    @Autowired
    public TodoService(TodoRepository todoRepository, UserRepository userRepository) {
        this.todoRepository = todoRepository;
        this.userRepository = userRepository;
    }

    public TodoResponseDTO createTask(Todo task, String email) {
        logger.debug("Creating task: {}", task);
        User user = getUser(email);

        if (taskExists(task, user)) {
            logger.warn("Task already exists for user: {}", email);
            throw new ApplicationException.TaskAlreadyExistsException("Task already exists for this user. Please modify the task details or check your tasks list");
        }

        task.setUser(user);
        Todo createdTask = todoRepository.save(task);
        logger.info("Task created successfully: {}", createdTask);

        UserDTO userDTO = new UserDTO(user.getId(), user.getEmail(), user.getName(), user.getRole());

        return new TodoResponseDTO(
                createdTask.getId(),
                createdTask.getTitle(),
                createdTask.getDescription(),
                createdTask.getPriority(),
                createdTask.getDueDate(),
                createdTask.isCompleted(),
                userDTO
        );
    }

    // Get all tasks for the authenticated user
    public Page<TodoResponseDTO> getAllTasks(String email, Pageable pageable) {
        logger.debug("Fetching all tasks for user: {}", email);
        return todoRepository.findAllByUserEmail(email, pageable)
                .map(this::convertToDTO);
    }


    // Get a task by ID
    public TodoResponseDTO getTaskById(Long id, String email) {
        logger.debug("Fetching task with id: {} for user: {}", id, email);
        User user = getUser(email);
        Todo todo = findTaskByIdAndUser(id, user);
        return convertToDTO(todo);
    }

    // Update a task
    public Todo updateTask(Long id, Todo updatedTodo, String email) {
        logger.debug("Updating task with id: {} for user: {}", id, email);
        User user = getUser(email);
        Todo existingTodo = todoRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Todo not found with id: {}", id);
                    return new ApplicationException.TodoNotFoundException("Todo not found");
                });

        // Ensure the task belongs to the user
        if (!existingTodo.getUser().getId().equals(user.getId())) {
            logger.error("Unauthorized access attempt for task id: {} by user: {}", id, email);
            throw new ApplicationException.UnauthorizedAccessException("You are not allowed to update this todo.");
        }

        existingTodo.setTitle(updatedTodo.getTitle());
        existingTodo.setDescription(updatedTodo.getDescription());
        existingTodo.setPriority(updatedTodo.getPriority());
        existingTodo.setDueDate(updatedTodo.getDueDate());
        existingTodo.setCompleted(updatedTodo.isCompleted());

        Todo updatedTask = todoRepository.save(existingTodo);
        logger.info("Task updated successfully: {}", updatedTask);
        return updatedTask;
    }

    // Delete a task
    public void deleteTask(Long id, String email) {
        logger.debug("Deleting task with id: {} for user: {}", id, email);
        User user = getUser(email);
        Todo task = findTaskByIdAndUser(id, user);

        // Ensure the task belongs to the user
        if (!task.getUser().getId().equals(user.getId())) {
            logger.error("Unauthorized access attempt for task id: {} by user: {}", id, email);
            throw new ApplicationException.UnauthorizedAccessException("You are not allowed to delete this todo.");
        }
        todoRepository.delete(task);
        logger.info("Task deleted successfully with id: {}", id);
    }

    // Get tasks by completed status
    public List<TodoResponseDTO> getTasksByCompletion(String email, boolean completed) {
        logger.debug("Fetching tasks for user: {} with completion status: {}", email, completed);
        User user = getUser(email);
        List<Todo> todos = todoRepository.findByUserAndCompleted(user, completed);
        return todos.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    // Get tasks by priority
    public Page<TodoResponseDTO> getTasksByPriority(String email, Priority priority, Pageable pageable) {
        logger.debug("Fetching tasks for user: {} with priority: {}", email, priority);
        User user = getUser(email);
        Page<Todo> todos = todoRepository.findByUserAndPriority(user, priority, pageable);
        return todos.map(this::convertToDTO);
    }

    // Get tasks by task title
    public Page<TodoResponseDTO> searchTasksByTitle(String email, String title, Pageable pageable) {
        logger.debug("Searching tasks for user: {} with title containing: {}", email, title);
        Page<Todo> todos = todoRepository.findByUserEmailAndTitleContainingIgnoreCase(email, title, pageable);

        if (todos.isEmpty()) {
            logger.info("No tasks found for user: {} with title: {}", email, title);
            return Page.empty();
        }

        return todos.map(this::convertToDTO);
    }

    // Get tasks due today for a specific user
    public Page<TodoResponseDTO> getTasksDueToday(String email, Pageable pageable) {
        logger.debug("Fetching tasks due today for user: {}", email);
        LocalDate today = LocalDate.now();
        Page<Todo> todos = todoRepository.findByUserEmailAndDueDate(email, today, pageable);

        if (todos.isEmpty()) {
            logger.info("No tasks due today for user: {}", email);
            return Page.empty();
        }

        return todos.map(this::convertToDTO);
    }


    // Get user details
    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.error("User not found for email: {}", email);
                    return new ApplicationException.UserNotFoundException("User not found");
                });
    }

    // Check task exit or not
    private boolean taskExists(Todo task, User user) {
        logger.debug("Checking if task exists: {} for user: {}", task.getTitle(), user.getEmail());
        return todoRepository.existsByTitleAndUser(task.getTitle(), user);
    }

    // Find task by userId
    private Todo findTaskByIdAndUser(Long id, User user) {
        return todoRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> {
                    logger.error("Todo not found with id: {} for user: {}", id, user.getEmail());
                    return new ApplicationException.TodoNotFoundException("Todo not found");
                });
    }

    //  Method to convert Task to TaskDTO
    private TodoResponseDTO convertToDTO(Todo task) {
        User user = task.getUser();
        UserDTO userDTO = new UserDTO(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole()
        );

        return new TodoResponseDTO(task.getId(), task.getTitle(), task.getDescription(),
                task.getPriority(), task.getDueDate(), task.isCompleted(), userDTO);
    }

}


