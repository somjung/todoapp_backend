package com.todoapp.service.strategy;

import org.springframework.stereotype.Component;

import com.todoapp.entity.Todo;

/**
 * Factory for creating todo completion strategies.
 * Follows Dependency Inversion Principle (DIP) - depends on abstractions, not concretions.
 * Follows Single Responsibility Principle (SRP) - only responsible for strategy creation.
 * Follows Open/Closed Principle (OCP) - can be extended with new strategies without modification.
 */
@Component
public class TodoStrategyFactory {
    
    private final StandardTodoStrategy standardStrategy;
    private final DeadlineTodoStrategy deadlineStrategy;
    private final SavingTodoStrategy savingStrategy;
    
    public TodoStrategyFactory(StandardTodoStrategy standardStrategy,
                              DeadlineTodoStrategy deadlineStrategy,
                              SavingTodoStrategy savingStrategy) {
        this.standardStrategy = standardStrategy;
        this.deadlineStrategy = deadlineStrategy;
        this.savingStrategy = savingStrategy;
    }
    
    /**
     * Gets the appropriate strategy for the given todo type.
     * @param type the todo type
     * @return the corresponding completion strategy
     * @throws IllegalArgumentException if todo type is not supported
     */
    public TodoCompletionStrategy getStrategy(Todo.TodoType type) {
        return switch (type) {
            case STANDARD -> standardStrategy;
            case DEADLINE -> deadlineStrategy;
            case SAVING -> savingStrategy;
        };
    }
    
    /**
     * Gets the appropriate strategy for the given todo.
     * @param todo the todo
     * @return the corresponding completion strategy
     */
    public TodoCompletionStrategy getStrategy(Todo todo) {
        return getStrategy(todo.getType());
    }
}