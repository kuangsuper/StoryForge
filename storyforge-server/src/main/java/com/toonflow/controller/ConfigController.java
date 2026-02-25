package com.toonflow.controller;

import com.toonflow.ai.model.TestResult;
import com.toonflow.common.ApiResponse;
import com.toonflow.dto.request.SaveConfigRequest;
import com.toonflow.dto.request.SaveModelMapRequest;
import com.toonflow.dto.response.ConfigResponse;
import com.toonflow.entity.AiModelMap;
import com.toonflow.service.ConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    @PostMapping("/models")
    public ApiResponse<Void> create(@Valid @RequestBody SaveConfigRequest request) {
        configService.create(request);
        return ApiResponse.ok();
    }

    @GetMapping("/models")
    public ApiResponse<List<ConfigResponse>> list() {
        return ApiResponse.ok(configService.list());
    }

    @PutMapping("/models/{id}")
    public ApiResponse<Void> update(@PathVariable Long id,
                                     @Valid @RequestBody SaveConfigRequest request) {
        configService.update(id, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/models/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        configService.delete(id);
        return ApiResponse.ok();
    }

    @GetMapping("/model-maps")
    public ApiResponse<List<AiModelMap>> getModelMaps() {
        return ApiResponse.ok(configService.getModelMaps());
    }

    @PutMapping("/model-maps")
    public ApiResponse<Void> updateModelMaps(@Valid @RequestBody SaveModelMapRequest request) {
        configService.updateModelMaps(request);
        return ApiResponse.ok();
    }

    @PostMapping("/models/{id}/test")
    public ApiResponse<TestResult> testConnectivity(@PathVariable Long id,
                                                     @RequestParam String type) {
        return ApiResponse.ok(configService.testConnectivity(id, type));
    }
}
