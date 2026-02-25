package com.toonflow.controller;

import com.toonflow.common.ApiResponse;
import com.toonflow.dto.request.SaveOutlineRequest;
import com.toonflow.entity.Outline;
import com.toonflow.service.OutlineService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/outlines")
@RequiredArgsConstructor
public class OutlineController {

    private final OutlineService outlineService;

    @GetMapping
    public ApiResponse<List<Outline>> list(@PathVariable Long projectId,
                                            @RequestParam(defaultValue = "full") String mode) {
        return ApiResponse.ok(outlineService.list(projectId, mode));
    }

    @PostMapping
    public ApiResponse<Outline> create(@PathVariable Long projectId,
                                        @RequestBody SaveOutlineRequest request) {
        return ApiResponse.ok(outlineService.create(projectId, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long projectId, @PathVariable Long id,
                                     @RequestBody SaveOutlineRequest request) {
        outlineService.update(id, request);
        return ApiResponse.ok();
    }

    @DeleteMapping
    public ApiResponse<Void> batchDelete(@PathVariable Long projectId,
                                          @RequestParam List<Long> ids) {
        outlineService.batchDelete(projectId, ids);
        return ApiResponse.ok();
    }

    @PostMapping("/extract-assets")
    public ApiResponse<Void> extractAssets(@PathVariable Long projectId) {
        outlineService.extractAssets(projectId);
        return ApiResponse.ok();
    }
}
