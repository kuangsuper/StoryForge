package com.toonflow.controller;

import com.toonflow.common.ApiResponse;
import com.toonflow.entity.Assets;
import com.toonflow.service.AssetsService;
import com.toonflow.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/assets")
@RequiredArgsConstructor
public class AssetsController {

    private final AssetsService assetsService;
    private final ImageService imageService;

    @GetMapping
    public ApiResponse<List<Assets>> list(@PathVariable Long projectId,
                                          @RequestParam(required = false) String type) {
        return ApiResponse.ok(assetsService.list(projectId, type));
    }

    @PostMapping
    public ApiResponse<Assets> create(@PathVariable Long projectId, @RequestBody Assets asset) {
        return ApiResponse.ok(assetsService.create(projectId, asset));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long projectId, @PathVariable Long id,
                                    @RequestBody Assets asset) {
        assetsService.update(id, asset);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long projectId, @PathVariable Long id) {
        assetsService.delete(id);
        return ApiResponse.ok();
    }

    @PostMapping("/batch")
    public ApiResponse<Void> batchSave(@PathVariable Long projectId,
                                       @RequestBody List<Assets> assets) {
        assetsService.batchSave(projectId, assets);
        return ApiResponse.ok();
    }

    @PostMapping("/extract")
    public ApiResponse<Void> extractFromOutlines(@PathVariable Long projectId) {
        assetsService.extractFromOutlines(projectId);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/polish-prompt")
    public ApiResponse<String> polishPrompt(@PathVariable Long projectId, @PathVariable Long id) {
        return ApiResponse.ok(assetsService.polishPrompt(id));
    }

    @PostMapping("/{id}/generate-image")
    public ApiResponse<com.toonflow.entity.Image> generateImage(@PathVariable Long projectId,
                                                                  @PathVariable Long id) {
        return ApiResponse.ok(imageService.generateAssetImage(projectId, id));
    }
}
