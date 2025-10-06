package com.todoapp.controller;

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

import com.todoapp.entity.TodoCollection;
import com.todoapp.entity.User;
import com.todoapp.repository.UserRepository;
import com.todoapp.service.TodoCollectionService;

@RestController
@RequestMapping("/api/collections")
@CrossOrigin(origins = "*")
public class TodoCollectionController {

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

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllCollections() {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "User not found"));
        }

        List<TodoCollection> collections = todoCollectionService.getAllCollectionsByUser(user);
        return ResponseEntity.ok(Map.of("success", true, "data", collections));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getCollection(@PathVariable Long id) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "User not found"));
        }

        Optional<TodoCollection> collection = todoCollectionService.getCollectionByIdAndUser(id, user);
        if (collection.isPresent()) {
            return ResponseEntity.ok(Map.of("success", true, "data", collection.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createCollection(@RequestBody Map<String, String> request) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "User not found"));
        }

        String name = request.get("name");
        String description = request.get("description");

        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Collection name is required"));
        }

        TodoCollection collection = todoCollectionService.createCollection(name, description, user);
        return ResponseEntity.ok(Map.of("success", true, "data", collection));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateCollection(@PathVariable Long id, @RequestBody Map<String, String> request) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "User not found"));
        }

        String name = request.get("name");
        String description = request.get("description");

        TodoCollection collection = todoCollectionService.updateCollection(id, name, description, user);
        if (collection != null) {
            return ResponseEntity.ok(Map.of("success", true, "data", collection));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteCollection(@PathVariable Long id) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "User not found"));
        }

        boolean deleted = todoCollectionService.deleteCollection(id, user);
        if (deleted) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Collection deleted successfully"));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}