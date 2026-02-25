package com.toonflow.controller;

import com.toonflow.common.ApiResponse;
import com.toonflow.entity.VideoConfig;
import com.toonflow.service.VideoConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/video-configs")
@RequiredArgsConstructor
public class VideoConfigController {

    private final VideoConfigService videoConfigService;

    @GetMapping
    public ApiResponse<List<VideoConfig>> list(@PathVariable Long projectId) {
        return ApiResponse.ok(videoConfigService.list(projectId));
    }

    @GetMapping("/{id}")
    public ApiResponse<VideoConfig> getById(@PathVariable Long projectId, @PathVariable Long id) {
        return ApiResponse.ok(videoConfigService.getById(id));
    }

    @PostMapping
    public ApiResponse<VideoConfig> save(@PathVariable Long projectId, @RequestBody VideoConfig config) {
        return ApiResponse.ok(videoConfigService.save(projectId, config));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long projectId, @PathVariable Long id) {
        videoConfigService.delete(id);
        return ApiResponse.ok();
    }
}
