package com.toonflow.controller;

import com.toonflow.common.ApiResponse;
import com.toonflow.entity.VideoCompose;
import com.toonflow.service.VideoComposeService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/videos/compose")
@RequiredArgsConstructor
public class VideoComposeController {

    private final VideoComposeService videoComposeService;

    @PostMapping
    public ApiResponse<VideoCompose> compose(@PathVariable Long projectId,
                                              @RequestBody ComposeRequest request) {
        return ApiResponse.ok(videoComposeService.submit(projectId, request.getScriptId(), request.getConfigId()));
    }

    @GetMapping("/{composeId}")
    public ApiResponse<VideoCompose> getById(@PathVariable Long projectId,
                                              @PathVariable Long composeId) {
        return ApiResponse.ok(videoComposeService.getById(composeId));
    }

    @PostMapping("/{composeId}/retry")
    public ApiResponse<Void> retry(@PathVariable Long projectId, @PathVariable Long composeId) {
        videoComposeService.retry(composeId);
        return ApiResponse.ok();
    }

    @GetMapping("/list")
    public ApiResponse<List<VideoCompose>> list(@PathVariable Long projectId,
                                                 @RequestParam Long scriptId) {
        return ApiResponse.ok(videoComposeService.listByScript(projectId, scriptId));
    }

    @Data
    static class ComposeRequest {
        private Long scriptId;
        private Long configId;
    }
}
