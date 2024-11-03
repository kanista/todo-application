package com.example.todo_project.repository;

import com.example.todo_project.entity.Priority;
import com.example.todo_project.entity.Todo;
import com.example.todo_project.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface TodoRepository extends JpaRepository<Todo,Long> {
    boolean existsByTitleAndUser(String title, User user);
    Page<Todo> findAllByUserEmail(String email, Pageable pageable);
    Optional<Todo> findByIdAndUser(Long id, User user);
    Page<Todo> findByUserAndCompleted(User user, boolean completed, Pageable pageable);
    Page<Todo> findByUserAndPriority(User user, Priority priority, Pageable pageable);
    Page<Todo> findByUserEmailAndTitleContainingIgnoreCase(String email, String title, Pageable pageable);
    Page<Todo> findByUserEmailAndDueDate(String email, LocalDate dueDate, Pageable pageable);


}
