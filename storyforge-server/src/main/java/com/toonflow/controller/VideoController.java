package com.toonflow.controller;

import com.toonflow.common.ApiResponse;
import com.toonflow.entity.Video;
import com.toonflow.service.VideoService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    @GetMapping
    public ApiResponse<List<Video>> list(@PathVariable Long projectId,
                                         @RequestParam Long scriptId) {
        return ApiResponse.ok(videoService.listByScript(scriptId));
    }

    @PostMapping("/generate")
    public ApiResponse<Video> generate(@PathVariable Long projectId,
                                       @RequestBody GenerateVideoRequest request) {
        return ApiResponse.ok(videoService.submitGenerate(projectId, request.getScriptId(),
                request.getShotId(), request.getConfigId(), request.getPrompt(), request.getImagePath()));
    }

    @PostMapping("/{id}/retry")
    public ApiResponse<Void> retry(@PathVariable Long projectId, @PathVariable Long id) {
        videoService.retryVideo(id);
        return ApiResponse.ok();
    }

    @PostMapping("/batch-retry")
    public ApiResponse<Void> batchRetry(@PathVariable Long projectId,
                                        @RequestParam Long scriptId) {
        videoService.batchRetry(projectId, scriptId);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/select")
    public ApiResponse<Void> selectVersion(@PathVariable Long projectId, @PathVariable Long id) {
        videoService.selectVersion(id);
        return ApiResponse.ok();
    }

    @GetMapping("/versions")
    public ApiResponse<List<Video>> getVersions(@PathVariable Long projectId,
                                                 @RequestParam String shotId) {
        return ApiResponse.ok(videoService.getVersions(shotId));
    }

    @PostMapping("/{id}/regenerate")
    public ApiResponse<Video> regenerate(@PathVariable Long projectId, @PathVariable Long id) {
        return ApiResponse.ok(videoService.regenerate(id));
    }

    @Data
    static class GenerateVideoRequest {
        private Long scriptId;
        private String shotId;
        private Long configId;
        private String prompt;
        private String imagePath;
    }
}
