package com.todoapp.service.strategy;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.todoapp.entity.Todo;

/**
 * Deadline todo completion strategy.
 * Follows Single Responsibility Principle (SRP) - only handles deadline todo completion logic.
 * Follows Liskov Substitution Principle (LSP) - can be substituted for TodoCompletionStrategy.
 */
@Component
public class DeadlineTodoStrategy implements TodoCompletionStrategy {
    
    @Override
    public boolean canComplete(Todo todo) {
        // Deadline todos can be completed even after the deadline
        return true;
    }
    
    @Override
    public void complete(Todo todo) {
        todo.setCompleted(true);
    }
    
    @Override
    public String getCompletionStatus(Todo todo) {
        if (todo.getCompleted()) {
            return isDeadlinePassed(todo) ? "COMPLETED_LATE" : "COMPLETED";
        }
        return isDeadlinePassed(todo) ? "FAILED" : "PENDING";
    }
    
    @Override
    public boolean shouldAutoComplete(Todo todo) {
        // Deadline todos are never auto-completed
        return false;
    }
    
    private boolean isDeadlinePassed(Todo todo) {
        return todo.getDueDate() != null && LocalDate.now().isAfter(todo.getDueDate());
    }
}