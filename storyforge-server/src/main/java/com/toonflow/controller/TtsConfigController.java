package com.toonflow.controller;

import com.toonflow.common.ApiResponse;
import com.toonflow.entity.TtsConfig;
import com.toonflow.service.TtsConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/tts/config")
@RequiredArgsConstructor
public class TtsConfigController {

    private final TtsConfigService ttsConfigService;

    @GetMapping
    public ApiResponse<List<TtsConfig>> list(@PathVariable Long projectId) {
        return ApiResponse.ok(ttsConfigService.list(projectId));
    }

    @PostMapping
    public ApiResponse<TtsConfig> create(@PathVariable Long projectId, @RequestBody TtsConfig config) {
        return ApiResponse.ok(ttsConfigService.create(projectId, config));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long projectId, @PathVariable Long id,
                                    @RequestBody TtsConfig config) {
        ttsConfigService.update(id, config);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long projectId, @PathVariable Long id) {
        ttsConfigService.delete(id);
        return ApiResponse.ok();
    }
}
