package com.todoapp.service.strategy;

import com.todoapp.entity.Todo;

/**
 * Strategy interface for handling different todo completion behaviors.
 * Follows Interface Segregation Principle (ISP) - clients only depend on methods they need.
 * Follows Open/Closed Principle (OCP) - open for extension, closed for modification.
 */
public interface TodoCompletionStrategy {
    
    /**
     * Determines if a todo can be completed based on its current state.
     * @param todo the todo to check
     * @return true if the todo can be completed, false otherwise
     */
    boolean canComplete(Todo todo);
    
    /**
     * Completes the todo according to its type-specific rules.
     * @param todo the todo to complete
     */
    void complete(Todo todo);
    
    /**
     * Gets the current completion status of the todo.
     * @param todo the todo to check
     * @return status string representing the current state
     */
    String getCompletionStatus(Todo todo);
    
    /**
     * Determines if the todo should be auto-completed based on business rules.
     * @param todo the todo to check
     * @return true if should auto-complete, false otherwise
     */
    boolean shouldAutoComplete(Todo todo);
}