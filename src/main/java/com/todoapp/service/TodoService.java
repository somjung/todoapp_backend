package com.todoapp.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.todoapp.entity.Todo;
import com.todoapp.entity.TodoCollection;
import com.todoapp.repository.TodoRepository;
import com.todoapp.service.strategy.TodoCompletionStrategy;
import com.todoapp.service.strategy.TodoStrategyFactory;

/**
 * Todo service following SOLID principles.
 * Follows Dependency Inversion Principle (DIP) - depends on abstractions (interfaces).
 * Follows Single Responsibility Principle (SRP) - only handles todo business operations.
 * Uses strategies for different todo behaviors (Open/Closed Principle).
 */
@Service
@Transactional
public class TodoService {

    private final TodoRepository todoRepository;
    private final TodoStrategyFactory strategyFactory;
    private final TodoCalculationService calculationService;

    // Constructor injection following DIP
    public TodoService(TodoRepository todoRepository,
                      TodoStrategyFactory strategyFactory,
                      TodoCalculationService calculationService) {
        this.todoRepository = todoRepository;
        this.strategyFactory = strategyFactory;
        this.calculationService = calculationService;
    }

    public List<Todo> getAllTodosByCollection(TodoCollection collection) {
        return todoRepository.findByCollectionOrderByCreatedAtDesc(collection);
    }

    public Optional<Todo> getTodoByIdAndCollection(Long id, TodoCollection collection) {
        return todoRepository.findByIdAndCollection(id, collection);
    }

    public Todo createTodo(Todo todo) {
        return todoRepository.save(todo);
    }

    public Todo updateTodo(Long id, Todo updatedTodo, TodoCollection collection) {
        Optional<Todo> todoOpt = todoRepository.findByIdAndCollection(id, collection);
        if (todoOpt.isPresent()) {
            Todo todo = todoOpt.get();
            todo.setTitle(updatedTodo.getTitle());
            todo.setDescription(updatedTodo.getDescription());
            todo.setCompleted(updatedTodo.getCompleted());
            todo.setType(updatedTodo.getType());
            todo.setDueDate(updatedTodo.getDueDate());
            todo.setTargetAmount(updatedTodo.getTargetAmount());
            todo.setCurrentAmount(updatedTodo.getCurrentAmount());
            
            // Apply auto-completion logic using strategy pattern
            TodoCompletionStrategy strategy = strategyFactory.getStrategy(todo);
            if (strategy.shouldAutoComplete(todo)) {
                strategy.complete(todo);
            }
            
            return todoRepository.save(todo);
        }
        return null;
    }

    /**
     * Completes a todo using the appropriate strategy.
     * Demonstrates Strategy Pattern and Open/Closed Principle.
     */
    public Todo completeTodo(Long id, TodoCollection collection) {
        Optional<Todo> todoOpt = todoRepository.findByIdAndCollection(id, collection);
        if (todoOpt.isPresent()) {
            Todo todo = todoOpt.get();
            TodoCompletionStrategy strategy = strategyFactory.getStrategy(todo);
            
            if (strategy.canComplete(todo)) {
                strategy.complete(todo);
                return todoRepository.save(todo);
            } else {
                throw new IllegalStateException("Todo cannot be completed in its current state");
            }
        }
        return null;
    }

    /**
     * Adds money to a saving goal and applies auto-completion logic.
     * Uses calculation service for business logic separation (SRP).
     */
    public Todo addMoneyToSavingGoal(Long id, BigDecimal amount, TodoCollection collection) {
        Optional<Todo> todoOpt = todoRepository.findByIdAndCollection(id, collection);
        if (todoOpt.isPresent()) {
            Todo todo = todoOpt.get();
            if (todo.getType() == Todo.TodoType.SAVING) {
                BigDecimal currentAmount = todo.getCurrentAmount() != null ? 
                    todo.getCurrentAmount() : BigDecimal.ZERO;
                todo.setCurrentAmount(currentAmount.add(amount));
                
                // Check if goal is reached using calculation service
                if (calculationService.isGoalReached(todo)) {
                    TodoCompletionStrategy strategy = strategyFactory.getStrategy(todo);
                    strategy.complete(todo);
                }
                
                return todoRepository.save(todo);
            }
        }
        return null;
    }

    public boolean deleteTodo(Long id, TodoCollection collection) {
        Optional<Todo> todoOpt = todoRepository.findByIdAndCollection(id, collection);
        if (todoOpt.isPresent()) {
            todoRepository.delete(todoOpt.get());
            return true;
        }
        return false;
    }
}