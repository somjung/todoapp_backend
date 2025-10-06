package com.todoapp.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;

import com.todoapp.entity.Todo;

/**
 * Service for todo calculations following Single Responsibility Principle (SRP).
 * Responsibility: Handle all calculation logic for todos.
 * Separated from Todo entity to follow separation of concerns.
 */
@Service
public class TodoCalculationService {
    
    /**
     * Calculates progress percentage for saving goals.
     * @param todo the saving goal todo
     * @return progress percentage as BigDecimal
     */
    public BigDecimal calculateProgress(Todo todo) {
        if (todo.getType() == Todo.TodoType.SAVING && 
            todo.getTargetAmount() != null && 
            todo.getTargetAmount().compareTo(BigDecimal.ZERO) > 0 &&
            todo.getCurrentAmount() != null) {
            
            return todo.getCurrentAmount()
                    .divide(todo.getTargetAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Checks if a saving goal has been reached.
     * @param todo the saving goal todo
     * @return true if goal is reached, false otherwise
     */
    public boolean isGoalReached(Todo todo) {
        return todo.getType() == Todo.TodoType.SAVING && 
               todo.getCurrentAmount() != null &&
               todo.getTargetAmount() != null &&
               todo.getCurrentAmount().compareTo(todo.getTargetAmount()) >= 0;
    }
    
    /**
     * Checks if a deadline has passed.
     * @param todo the deadline todo
     * @return true if deadline has passed, false otherwise
     */
    public boolean isDeadlinePassed(Todo todo) {
        return todo.getType() == Todo.TodoType.DEADLINE && 
               todo.getDueDate() != null && 
               LocalDate.now().isAfter(todo.getDueDate());
    }
    
    /**
     * Calculates days remaining until deadline.
     * @param todo the deadline todo
     * @return days remaining (negative if overdue)
     */
    public long getDaysUntilDeadline(Todo todo) {
        if (todo.getType() == Todo.TodoType.DEADLINE && todo.getDueDate() != null) {
            return ChronoUnit.DAYS.between(LocalDate.now(), todo.getDueDate());
        }
        return 0;
    }
    
    /**
     * Calculates remaining amount needed for saving goal.
     * @param todo the saving goal todo
     * @return remaining amount needed
     */
    public BigDecimal getRemainingAmount(Todo todo) {
        if (todo.getType() == Todo.TodoType.SAVING && 
            todo.getTargetAmount() != null && 
            todo.getCurrentAmount() != null) {
            
            BigDecimal remaining = todo.getTargetAmount().subtract(todo.getCurrentAmount());
            return remaining.max(BigDecimal.ZERO);
        }
        return BigDecimal.ZERO;
    }
}