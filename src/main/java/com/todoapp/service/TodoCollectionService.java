package com.todoapp.service;

import com.todoapp.entity.TodoCollection;
import com.todoapp.entity.User;
import com.todoapp.repository.TodoCollectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TodoCollectionService {

    @Autowired
    private TodoCollectionRepository todoCollectionRepository;

    public List<TodoCollection> getAllCollectionsByUser(User user) {
        return todoCollectionRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public Optional<TodoCollection> getCollectionByIdAndUser(Long id, User user) {
        return todoCollectionRepository.findByIdAndUser(id, user);
    }

    public TodoCollection createCollection(String name, String description, User user) {
        TodoCollection collection = new TodoCollection();
        collection.setName(name);
        collection.setDescription(description);
        collection.setUser(user);
        return todoCollectionRepository.save(collection);
    }

    public TodoCollection updateCollection(Long id, String name, String description, User user) {
        Optional<TodoCollection> collectionOpt = todoCollectionRepository.findByIdAndUser(id, user);
        if (collectionOpt.isPresent()) {
            TodoCollection collection = collectionOpt.get();
            collection.setName(name);
            collection.setDescription(description);
            return todoCollectionRepository.save(collection);
        }
        return null;
    }

    public boolean deleteCollection(Long id, User user) {
        Optional<TodoCollection> collectionOpt = todoCollectionRepository.findByIdAndUser(id, user);
        if (collectionOpt.isPresent()) {
            todoCollectionRepository.delete(collectionOpt.get());
            return true;
        }
        return false;
    }
}