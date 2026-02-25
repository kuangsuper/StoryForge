package com.toonflow.controller;

import com.toonflow.common.ApiResponse;
import com.toonflow.entity.Material;
import com.toonflow.service.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    @GetMapping
    public ApiResponse<List<Material>> list(@RequestParam(required = false) String type,
                                            @RequestParam(required = false) String category) {
        return ApiResponse.ok(materialService.list(type, category));
    }

    /**
     * 上传素材（multipart/form-data）
     */
    @PostMapping(consumes = "multipart/form-data")
    public ApiResponse<Material> upload(@RequestParam("file") MultipartFile file,
                                        @RequestParam String name,
                                        @RequestParam String type,
                                        @RequestParam(required = false) String category,
                                        @RequestParam(required = false) Long userId) throws IOException {
        return ApiResponse.ok(materialService.upload(
                file.getBytes(),
                file.getOriginalFilename(),
                name, type, category, userId));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        materialService.delete(id);
        return ApiResponse.ok();
    }
}
