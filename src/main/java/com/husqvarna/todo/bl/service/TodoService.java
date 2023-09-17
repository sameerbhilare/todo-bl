package com.husqvarna.todo.bl.service;

import com.husqvarna.todo.bl.common.ErrorCode;
import com.husqvarna.todo.bl.common.MessageConstants;
import com.husqvarna.todo.bl.common.TodoStatus;
import com.husqvarna.todo.bl.db.entity.Todo;
import com.husqvarna.todo.bl.db.repository.TodoRepository;
import com.husqvarna.todo.bl.exception.BusinessException;
import com.husqvarna.todo.bl.vo.TodoVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Service
public class TodoService {

    @Autowired
    private TodoRepository todoRepository;

    public List<TodoVO> getTodos(String status) {

        TodoStatus todoStatus = TodoStatus.fromValue(status);
        if (status != null && todoStatus == null) {
            throw new BusinessException(ErrorCode.INVALID_STATUS.name(), MessageConstants.INVALID_STATUS);
        }

        // fetch todos from database either based on status or all (if status is not provided input)
        List<Todo> fetchedTodos = Optional.ofNullable(status)
                .map(s -> todoRepository.findByStatus(todoStatus))
                .orElse(todoRepository.findAll());

        // map JPA entity to value object
        return Optional.of(fetchedTodos)
                .filter(CollectionUtils::isNotEmpty)
                .orElse(Collections.emptyList())
                .stream()
                .map(mapToDoVO)
                .toList();
    }

    private static final Function<Todo, TodoVO> mapToDoVO = todo -> TodoVO.builder()
            .id(todo.getId())
            .name(todo.getName())
            .status(todo.getStatus().name())
            .build();

    public TodoVO getTodo(Long todoId) {

        return todoRepository.findById(todoId)
                .map(mapToDoVO)
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND.name(), MessageConstants.TASK_NOT_FOUND));
    }

    public void deleteTodo(Long todoId) {

        // if todoId not present, throw business exception
        validateIfTodoPresent(todoId);

        // delete task
        todoRepository.deleteById(todoId);
    }

    public void updateTodo(Long todoId, TodoVO todoVO) {

        // if todoId not present, throw business exception
        Todo todoToBeUpdated = validateIfTodoPresent(todoId);

        TodoStatus updatedStatus = TodoStatus.fromValue(todoVO.getStatus());
        if (todoVO.getStatus() != null && updatedStatus == null) {
            throw new BusinessException(ErrorCode.INVALID_STATUS.name(), MessageConstants.INVALID_STATUS);
        }

        // update the task
        todoToBeUpdated.setName(todoVO.getName());
        todoToBeUpdated.setStatus(updatedStatus);

        // save updated task in the db
        todoRepository.save(todoToBeUpdated);
    }

    private Todo validateIfTodoPresent(Long todoId) {
        // if todoId not present, throw business exception
        return todoRepository.findById(todoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND.name(), MessageConstants.TASK_NOT_FOUND));
    }

    public void deleteAllTodos() {
        todoRepository.deleteAll();
    }

    public TodoVO createTodo(TodoVO todoVO) {

        // create Todo_ entity
        Todo todo = new Todo();
        todo.setName(todoVO.getName());
        todo.setStatus(TodoStatus.ACTIVE);

        // save in the db and return mapped object
        return Optional.of(todoRepository.save(todo))
                .map(mapToDoVO)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNABLE_TO_CREATE.name(), MessageConstants.UNABLE_TO_CREATE));
    }
}
