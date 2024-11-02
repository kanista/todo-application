package com.example.todo_project.service;

import com.example.todo_project.dto.TodoResponseDTO;
import com.example.todo_project.dto.UserDTO;
import com.example.todo_project.entity.Priority;
import com.example.todo_project.entity.Todo;
import com.example.todo_project.entity.User;
import com.example.todo_project.exception.ApplicationException;
import com.example.todo_project.repository.TodoRepository;
import com.example.todo_project.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TodoService {

    private final TodoRepository todoRepository;
    private final UserRepository userRepository;

    @Autowired
    public TodoService(TodoRepository todoRepository, UserRepository userRepository) {
        this.todoRepository = todoRepository;
        this.userRepository = userRepository;
    }

    public TodoResponseDTO createTask(Todo task, String email) {
        User user = getUser(email);
        if (taskExists(task, user)) {
            throw new ApplicationException.TaskAlreadyExistsException("Task already exists for this user. Please modify the task details or check your tasks list");
        }

        task.setUser(user);
        Todo createdTask = todoRepository.save(task);

        UserDTO userDTO = new UserDTO(user.getId(), user.getEmail(), user.getName(),user.getRole());

        // Return TaskResponseDto
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
        // Fetch paginated tasks and convert to DTO
        return todoRepository.findAllByUserEmail(email, pageable)
                .map(this::convertToDTO);
    }


    // Get a task by ID
    public TodoResponseDTO getTaskById(Long id, String email) {
        User user = getUser(email);
        Todo todo = findTaskByIdAndUser(id, user);
        return convertToDTO(todo);
    }

    // Update a task
    public Todo updateTask(Long id, Todo updatedTodo, String email) {
        User user = getUser(email);

        Todo existingTodo = todoRepository.findById(id)
                .orElseThrow(() -> new ApplicationException.TodoNotFoundException("Todo not found"));

        // Ensure the post belongs to the user
        if (!existingTodo.getUser().getId().equals(user.getId())) {
            throw new ApplicationException.UnauthorizedAccessException("You are not allowed to update this todo.");
        }

        existingTodo.setTitle(updatedTodo.getTitle());
        existingTodo.setDescription(updatedTodo.getDescription());
        existingTodo.setPriority(updatedTodo.getPriority());
        existingTodo.setDueDate(updatedTodo.getDueDate());
        existingTodo.setCompleted(updatedTodo.isCompleted());

        return todoRepository.save(existingTodo);
    }

    // Delete a task
    public void deleteTask(Long id, String email) {
        User user = getUser(email);
        Todo task = findTaskByIdAndUser(id, user);
        // Ensure the post belongs to the user
        if (!task.getUser().getId().equals(user.getId())) {
            throw new ApplicationException.UnauthorizedAccessException("You are not allowed to delete this todo.");
        }
        todoRepository.delete(task);
    }


    public List<TodoResponseDTO> getTasksByCompletion(String email, boolean completed) {
        User user = getUser(email);
        List<Todo> todos = todoRepository.findByUserAndCompleted(user, completed);
        return todos.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public Page<TodoResponseDTO> getTasksByPriority(String email, Priority priority, Pageable pageable) {
        User user = getUser(email);
        Page<Todo> todos = todoRepository.findByUserAndPriority(user, priority, pageable);
        return todos.map(this::convertToDTO);
    }



    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApplicationException.UserNotFoundException("User not found"));
    }

    private boolean taskExists(Todo task, User user) {
        // Check for tasks with the same title or other identifying attributes
        return todoRepository.existsByTitleAndUser(task.getTitle(), user);
    }

    private Todo findTaskByIdAndUser(Long id, User user) {
        return todoRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ApplicationException.TodoNotFoundException("Todo not found"));
    }

    // Helper method to convert Task to TaskDTO
    private TodoResponseDTO convertToDTO(Todo task) {
        User user = task.getUser(); // Get the User object
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


