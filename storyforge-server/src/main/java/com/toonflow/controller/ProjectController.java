package com.toonflow.controller;

import com.toonflow.common.ApiResponse;
import com.toonflow.common.PageResult;
import com.toonflow.dto.request.CreateProjectRequest;
import com.toonflow.dto.request.UpdateProjectRequest;
import com.toonflow.entity.Project;
import com.toonflow.security.SecurityUtil;
import com.toonflow.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ApiResponse<Project> create(@Valid @RequestBody CreateProjectRequest request) {
        return ApiResponse.ok(projectService.create(SecurityUtil.getCurrentUserId(), request));
    }

    @GetMapping
    public ApiResponse<PageResult<Project>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(projectService.list(SecurityUtil.getCurrentUserId(), page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<Project> getById(@PathVariable Long id) {
        return ApiResponse.ok(projectService.getById(id, SecurityUtil.getCurrentUserId()));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id,
                                     @Valid @RequestBody UpdateProjectRequest request) {
        projectService.update(id, SecurityUtil.getCurrentUserId(), request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        projectService.delete(id, SecurityUtil.getCurrentUserId());
        return ApiResponse.ok();
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Long>> stats() {
        long count = projectService.stats(SecurityUtil.getCurrentUserId());
        return ApiResponse.ok(Map.of("total", count));
    }
}
