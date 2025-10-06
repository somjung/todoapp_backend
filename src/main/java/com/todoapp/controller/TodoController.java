package com.todoapp.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.todoapp.entity.Todo;
import com.todoapp.entity.TodoCollection;
import com.todoapp.entity.User;
import com.todoapp.repository.UserRepository;
import com.todoapp.service.TodoCollectionService;
import com.todoapp.service.TodoService;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class TodoController {

    @Autowired
    private TodoService todoService;

    @Autowired
    private TodoCollectionService todoCollectionService;

    @Autowired
    private UserRepository userRepository;

    // Get the current authenticated user from JWT token
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping("/collections/{collectionId}/tasks")
    public ResponseEntity<Map<String, Object>> createTodo(@PathVariable Long collectionId, @RequestBody Map<String, Object> request) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "User not found"));
        }

        Optional<TodoCollection> collectionOpt = todoCollectionService.getCollectionByIdAndUser(collectionId, user);
        if (!collectionOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        TodoCollection collection = collectionOpt.get();

        try {
            Todo todo = new Todo();
            todo.setTitle((String) request.get("title"));
            todo.setDescription((String) request.get("description"));
            todo.setCollection(collection);

            String type = (String) request.get("type");
            if (type != null) {
                todo.setType(Todo.TodoType.valueOf(type.toUpperCase()));
            }

            // Handle type-specific fields
            if (todo.getType() == Todo.TodoType.DEADLINE && request.get("dueDate") != null) {
                todo.setDueDate(LocalDate.parse((String) request.get("dueDate")));
            }

            if (todo.getType() == Todo.TodoType.SAVING) {
                if (request.get("targetAmount") != null) {
                    todo.setTargetAmount(new BigDecimal(request.get("targetAmount").toString()));
                }
                if (request.get("currentAmount") != null) {
                    todo.setCurrentAmount(new BigDecimal(request.get("currentAmount").toString()));
                }
            }

            Todo savedTodo = todoService.createTodo(todo);
            return ResponseEntity.ok(Map.of("success", true, "data", savedTodo));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Error creating todo: " + e.getMessage()));
        }
    }

    @GetMapping("/collections/{collectionId}/tasks")
    public ResponseEntity<Map<String, Object>> getTodosByCollection(@PathVariable Long collectionId) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "User not found"));
        }

        Optional<TodoCollection> collectionOpt = todoCollectionService.getCollectionByIdAndUser(collectionId, user);
        if (!collectionOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        List<Todo> todos = todoService.getAllTodosByCollection(collectionOpt.get());
        return ResponseEntity.ok(Map.of("success", true, "data", todos));
    }

    @PostMapping("/tasks/{taskId}/add-money")
    public ResponseEntity<Map<String, Object>> addMoneyToSavingGoal(@PathVariable Long taskId, @RequestBody Map<String, Object> request) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "User not found"));
        }

        try {
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            
            // We need to find the collection that contains this task
            // For now, we'll search through all user's collections
            List<TodoCollection> collections = todoCollectionService.getAllCollectionsByUser(user);
            Todo updatedTodo = null;

            for (TodoCollection collection : collections) {
                updatedTodo = todoService.addMoneyToSavingGoal(taskId, amount, collection);
                if (updatedTodo != null) {
                    break;
                }
            }

            if (updatedTodo != null) {
                return ResponseEntity.ok(Map.of("success", true, "data", updatedTodo));
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Error adding money: " + e.getMessage()));
        }
    }

    @PutMapping("/tasks/{taskId}")
    public ResponseEntity<Map<String, Object>> updateTodo(@PathVariable Long taskId, @RequestBody Map<String, Object> request) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "User not found"));
        }

        try {
            // Find the collection that contains this task
            List<TodoCollection> collections = todoCollectionService.getAllCollectionsByUser(user);
            Todo updatedTodo = null;

            for (TodoCollection collection : collections) {
                Optional<Todo> todoOpt = todoService.getTodoByIdAndCollection(taskId, collection);
                if (todoOpt.isPresent()) {
                    Todo todo = new Todo();
                    todo.setTitle((String) request.get("title"));
                    todo.setDescription((String) request.get("description"));
                    todo.setCompleted((Boolean) request.get("completed"));
                    
                    updatedTodo = todoService.updateTodo(taskId, todo, collection);
                    break;
                }
            }

            if (updatedTodo != null) {
                return ResponseEntity.ok(Map.of("success", true, "data", updatedTodo));
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Error updating todo: " + e.getMessage()));
        }
    }

    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<Map<String, Object>> deleteTodo(@PathVariable Long taskId) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "User not found"));
        }

        // Find the collection that contains this task
        List<TodoCollection> collections = todoCollectionService.getAllCollectionsByUser(user);
        boolean deleted = false;

        for (TodoCollection collection : collections) {
            if (todoService.deleteTodo(taskId, collection)) {
                deleted = true;
                break;
            }
        }

        if (deleted) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Todo deleted successfully"));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}