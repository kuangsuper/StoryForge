package com.toonflow.controller;

import com.toonflow.common.ApiResponse;
import com.toonflow.dto.request.AddNovelRequest;
import com.toonflow.dto.request.UpdateNovelRequest;
import com.toonflow.dto.response.NovelListResponse;
import com.toonflow.entity.Novel;
import com.toonflow.service.NovelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/novels")
@RequiredArgsConstructor
public class NovelController {

    private final NovelService novelService;

    @PostMapping
    public ApiResponse<Void> create(@PathVariable Long projectId,
                                     @Valid @RequestBody AddNovelRequest request) {
        novelService.batchCreate(projectId, request);
        return ApiResponse.ok();
    }

    @GetMapping
    public ApiResponse<List<NovelListResponse>> list(@PathVariable Long projectId) {
        return ApiResponse.ok(novelService.list(projectId));
    }

    @GetMapping("/{id}")
    public ApiResponse<Novel> getById(@PathVariable Long projectId, @PathVariable Long id) {
        return ApiResponse.ok(novelService.getById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long projectId, @PathVariable Long id,
                                     @RequestBody UpdateNovelRequest request) {
        novelService.update(id, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long projectId, @PathVariable Long id) {
        novelService.delete(id);
        return ApiResponse.ok();
    }
}
