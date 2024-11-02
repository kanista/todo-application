package com.example.todo_project.repository;

import com.example.todo_project.entity.Todo;
import com.example.todo_project.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TodoRepository extends JpaRepository<Todo,Long> {
    boolean existsByTitleAndUser(String title, User user);
    Page<Todo> findAllByUserEmail(String email, Pageable pageable);
    Page<Todo> findAllByUserEmailAndTitleContainingIgnoreCase(String email, String keyword, Pageable pageable);
    Optional<Todo> findByIdAndUser(Long id, User user);
    List<Todo> findByUserAndCompleted(User user, boolean completed);

}
