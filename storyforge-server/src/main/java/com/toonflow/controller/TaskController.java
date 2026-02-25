package com.toonflow.controller;

import com.toonflow.common.ApiResponse;
import com.toonflow.common.PageResult;
import com.toonflow.entity.TaskList;
import com.toonflow.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public ApiResponse<PageResult<TaskList>> list(@RequestParam(required = false) String state,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(taskService.list(state, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<TaskList> getById(@PathVariable Long id) {
        return ApiResponse.ok(taskService.getById(id));
    }
}
