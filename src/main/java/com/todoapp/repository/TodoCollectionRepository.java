package com.todoapp.repository;

import com.todoapp.entity.TodoCollection;
import com.todoapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TodoCollectionRepository extends JpaRepository<TodoCollection, Long> {
    List<TodoCollection> findByUserOrderByCreatedAtDesc(User user);
    Optional<TodoCollection> findByIdAndUser(Long id, User user);
}