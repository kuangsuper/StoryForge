package com.toonflow.controller;

import com.toonflow.agent.storyboard.Segment;
import com.toonflow.agent.storyboard.Shot;
import com.toonflow.common.ApiResponse;
import com.toonflow.service.StoryboardService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/storyboard")
@RequiredArgsConstructor
public class StoryboardController {

    private final StoryboardService storyboardService;

    @GetMapping("/{scriptId}")
    public ApiResponse<Map<String, Object>> get(@PathVariable Long projectId,
                                                 @PathVariable Long scriptId) {
        return ApiResponse.ok(storyboardService.getByScript(projectId, scriptId));
    }

    @PostMapping("/{scriptId}/save")
    public ApiResponse<Void> save(@PathVariable Long projectId, @PathVariable Long scriptId,
                                  @RequestBody SaveStoryboardRequest request) {
        storyboardService.save(projectId, scriptId, request.getSegments(), request.getShots());
        return ApiResponse.ok();
    }

    @PostMapping("/{scriptId}/confirm")
    public ApiResponse<Void> confirm(@PathVariable Long projectId, @PathVariable Long scriptId) {
        storyboardService.confirm(projectId, scriptId);
        return ApiResponse.ok();
    }

    @Data
    static class SaveStoryboardRequest {
        private List<Segment> segments;
        private List<Shot> shots;
    }
}
