package com.todoapp.service.strategy;

import org.springframework.stereotype.Component;

import com.todoapp.entity.Todo;

/**
 * Saving goal todo completion strategy.
 * Follows Single Responsibility Principle (SRP) - only handles saving goal completion logic.
 * Follows Liskov Substitution Principle (LSP) - can be substituted for TodoCompletionStrategy.
 */
@Component
public class SavingTodoStrategy implements TodoCompletionStrategy {
    
    @Override
    public boolean canComplete(Todo todo) {
        // Can be completed manually or when goal is reached
        return true;
    }
    
    @Override
    public void complete(Todo todo) {
        todo.setCompleted(true);
    }
    
    @Override
    public String getCompletionStatus(Todo todo) {
        if (todo.getCompleted()) {
            return "GOAL_REACHED";
        }
        return shouldAutoComplete(todo) ? "GOAL_REACHED" : "IN_PROGRESS";
    }
    
    @Override
    public boolean shouldAutoComplete(Todo todo) {
        return todo.getTargetAmount() != null && 
               todo.getCurrentAmount() != null &&
               todo.getCurrentAmount().compareTo(todo.getTargetAmount()) >= 0;
    }
}