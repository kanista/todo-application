package com.example.todo_project;

import com.example.todo_project.dto.TodoResponseDTO;
import com.example.todo_project.entity.Priority;
import com.example.todo_project.entity.Role;
import com.example.todo_project.entity.Todo;
import com.example.todo_project.entity.User;
import com.example.todo_project.exception.ApplicationException;
import com.example.todo_project.repository.TodoRepository;
import com.example.todo_project.repository.UserRepository;
import com.example.todo_project.service.TodoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.time.LocalDate;
import java.util.Optional;
import org.mockito.MockitoAnnotations;

public class TodoServiceTest {
    @InjectMocks
    private TodoService todoService;

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private UserRepository userRepository;

    private AutoCloseable closeable;  // To manage Mockito's openMocks resource

    private User user;
    private Todo todo;

    @BeforeEach
    void setUp() {
        // Initialize mocks and capture AutoCloseable for proper resource management
        closeable = MockitoAnnotations.openMocks(this);

        // Set up test data
        user = new User(1L, "test@example.com", "password", "Test User", Role.USER);
        todo = new Todo(1L, "Test 1", "java", null, Priority.LOW, false, user);
    }

    @AfterEach
    void tearDown() throws Exception {
        // Close AutoCloseable to release resources after each test
        if (closeable != null) {
            closeable.close();
        }
    }

    @Test
    void createTask_Success() {
        // Given
        String email = "test@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(todoRepository.existsByTitleAndUser(todo.getTitle(), user)).thenReturn(false);
        when(todoRepository.save(todo)).thenReturn(todo);

        // When
        TodoResponseDTO createdTask = todoService.createTask(todo, email);

        // Then
        assertNotNull(createdTask);
        System.out.println("Task created with ID: " + createdTask.getId()); // Output for debugging
        assertEquals(todo.getId(), createdTask.getId());
        assertEquals(todo.getTitle(), createdTask.getTitle());
        verify(todoRepository, times(1)).save(todo);
    }

    @Test
    void updateTask_Success() {
        // Given
        String email = "test@example.com";
        Todo updatedTodo = new Todo();
        updatedTodo.setId(todo.getId());
        updatedTodo.setTitle("Updated Test 1");
        updatedTodo.setDescription("Updated Description");
        updatedTodo.setPriority(Priority.HIGH);
        updatedTodo.setDueDate(LocalDate.now().plusDays(1));
        updatedTodo.setCompleted(false);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(todoRepository.findById(todo.getId())).thenReturn(Optional.of(todo));
        when(todoRepository.save(any(Todo.class))).thenReturn(updatedTodo);

        // When
        Todo updatedTask = todoService.updateTask(todo.getId(), updatedTodo, email);

        // Then
        assertNotNull(updatedTask);
        System.out.println("Task updated with ID: " + updatedTask.getTitle());
        assertEquals(updatedTodo.getTitle(), updatedTask.getTitle());
        verify(todoRepository, times(1)).save(any(Todo.class));
    }



    @Test
    void createTask_TaskAlreadyExists() {
        // Given
        String email = "test@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(todoRepository.existsByTitleAndUser(todo.getTitle(), user)).thenReturn(true);

        // When & Then
        Exception exception = assertThrows(ApplicationException.TaskAlreadyExistsException.class, () -> {
            todoService.createTask(todo, email);
        });

        assertEquals("Task already exists for this user. Please modify the task details or check your tasks list", exception.getMessage());
    }


}
