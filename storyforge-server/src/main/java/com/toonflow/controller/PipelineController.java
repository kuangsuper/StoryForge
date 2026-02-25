package com.toonflow.controller;

import com.toonflow.common.ApiResponse;
import com.toonflow.dto.request.PipelineRequest;
import com.toonflow.dto.response.PipelineStatus;
import com.toonflow.service.pipeline.PipelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/pipeline")
@RequiredArgsConstructor
public class PipelineController {

    private final PipelineService pipelineService;

    @PostMapping("/start")
    public ApiResponse<Long> start(@PathVariable Long projectId, @RequestBody PipelineRequest request) {
        request.setProjectId(projectId);
        return ApiResponse.ok(pipelineService.startPipeline(projectId, request));
    }

    @PostMapping("/batch")
    public ApiResponse<List<Long>> batch(@RequestBody List<PipelineRequest> requests) {
        return ApiResponse.ok(pipelineService.startBatch(requests));
    }

    @GetMapping("/status")
    public ApiResponse<PipelineStatus> status(@PathVariable Long projectId) {
        return ApiResponse.ok(pipelineService.getStatus(projectId));
    }

    @PostMapping("/retry")
    public ApiResponse<Void> retry(@PathVariable Long projectId) {
        pipelineService.retryStep(projectId);
        return ApiResponse.ok();
    }

    @PostMapping("/skip")
    public ApiResponse<Void> skip(@PathVariable Long projectId) {
        pipelineService.skipStep(projectId);
        return ApiResponse.ok();
    }

    @PostMapping("/terminate")
    public ApiResponse<Void> terminate(@PathVariable Long projectId) {
        pipelineService.terminate(projectId);
        return ApiResponse.ok();
    }

    @PostMapping("/approve")
    public ApiResponse<Void> approve(@PathVariable Long projectId) {
        pipelineService.approveReview(projectId);
        return ApiResponse.ok();
    }
}
