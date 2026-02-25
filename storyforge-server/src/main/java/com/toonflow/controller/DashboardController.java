package com.toonflow.controller;

import com.toonflow.common.ApiResponse;
import com.toonflow.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.ok(dashboardService.getStats());
    }

    @GetMapping("/daily")
    public ApiResponse<Map<String, Object>> daily() {
        return ApiResponse.ok(dashboardService.getDailyMetrics());
    }

    @GetMapping("/models")
    public ApiResponse<Map<String, Object>> models() {
        return ApiResponse.ok(dashboardService.getModelUsage());
    }

    @GetMapping("/pipelines")
    public ApiResponse<Map<String, Object>> pipelines() {
        return ApiResponse.ok(dashboardService.getPipelineStats());
    }
}
