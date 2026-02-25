package com.toonflow.controller;

import com.toonflow.common.ApiResponse;
import com.toonflow.dto.request.UpdatePromptRequest;
import com.toonflow.entity.Prompts;
import com.toonflow.service.PromptService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prompts")
@RequiredArgsConstructor
public class PromptController {

    private final PromptService promptService;

    @GetMapping
    public ApiResponse<List<Prompts>> list() {
        return ApiResponse.ok(promptService.list());
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id,
                                     @RequestBody UpdatePromptRequest request) {
        promptService.updateCustomValue(id, request.getCustomValue());
        return ApiResponse.ok();
    }
}
