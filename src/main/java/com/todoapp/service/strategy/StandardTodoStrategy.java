package com.todoapp.service.strategy;

import org.springframework.stereotype.Component;

import com.todoapp.entity.Todo;

/**
 * Standard todo completion strategy.
 * Follows Single Responsibility Principle (SRP) - only handles standard todo completion logic.
 * Follows Liskov Substitution Principle (LSP) - can be substituted for TodoCompletionStrategy.
 */
@Component
public class StandardTodoStrategy implements TodoCompletionStrategy {
    
    @Override
    public boolean canComplete(Todo todo) {
        // Standard todos can always be completed manually
        return true;
    }
    
    @Override
    public void complete(Todo todo) {
        todo.setCompleted(true);
    }
    
    @Override
    public String getCompletionStatus(Todo todo) {
        return todo.getCompleted() ? "COMPLETED" : "PENDING";
    }
    
    @Override
    public boolean shouldAutoComplete(Todo todo) {
        // Standard todos are never auto-completed
        return false;
    }
}