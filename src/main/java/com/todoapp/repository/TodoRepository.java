package com.todoapp.repository;

import com.todoapp.entity.Todo;
import com.todoapp.entity.TodoCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TodoRepository extends JpaRepository<Todo, Long> {
    List<Todo> findByCollectionOrderByCreatedAtDesc(TodoCollection collection);
    Optional<Todo> findByIdAndCollection(Long id, TodoCollection collection);
}