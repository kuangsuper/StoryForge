package com.toonflow.controller;

import com.toonflow.common.ApiResponse;
import com.toonflow.dto.request.SelectAssetsRequest;
import com.toonflow.entity.Assets;
import com.toonflow.entity.Image;
import com.toonflow.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @GetMapping
    public ApiResponse<List<Image>> list(@PathVariable Long projectId,
                                         @RequestParam(required = false) String type,
                                         @RequestParam(required = false) Long assetsId) {
        return ApiResponse.ok(imageService.list(projectId, type, assetsId));
    }

    @PostMapping("/select-assets")
    public ApiResponse<List<Assets>> selectRelevantAssets(@PathVariable Long projectId,
                                                          @RequestBody SelectAssetsRequest request) {
        return ApiResponse.ok(imageService.selectRelevantAssets(projectId, request.getShotPrompts()));
    }
}
