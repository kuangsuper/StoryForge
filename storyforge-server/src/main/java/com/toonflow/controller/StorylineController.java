package com.toonflow.controller;

import com.toonflow.common.ApiResponse;
import com.toonflow.dto.request.SaveStorylineRequest;
import com.toonflow.entity.Storyline;
import com.toonflow.service.StorylineService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}/storylines")
@RequiredArgsConstructor
public class StorylineController {

    private final StorylineService storylineService;

    @GetMapping
    public ApiResponse<Storyline> get(@PathVariable Long projectId) {
        return ApiResponse.ok(storylineService.get(projectId));
    }

    @PutMapping
    public ApiResponse<Void> save(@PathVariable Long projectId,
                                   @RequestBody SaveStorylineRequest request) {
        storylineService.saveOrUpdate(projectId, request);
        return ApiResponse.ok();
    }

    @DeleteMapping
    public ApiResponse<Void> delete(@PathVariable Long projectId) {
        storylineService.delete(projectId);
        return ApiResponse.ok();
    }
}
