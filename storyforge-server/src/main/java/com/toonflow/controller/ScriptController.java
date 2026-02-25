package com.toonflow.controller;

import com.toonflow.common.ApiResponse;
import com.toonflow.dto.request.GenerateScriptRequest;
import com.toonflow.dto.request.UpdateScriptRequest;
import com.toonflow.entity.Script;
import com.toonflow.service.ScriptService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/scripts")
@RequiredArgsConstructor
public class ScriptController {

    private final ScriptService scriptService;

    @GetMapping("/{id}")
    public ApiResponse<Script> getById(@PathVariable Long projectId, @PathVariable Long id) {
        return ApiResponse.ok(scriptService.getById(id));
    }

    @GetMapping
    public ApiResponse<List<Script>> list(@PathVariable Long projectId) {
        return ApiResponse.ok(scriptService.listByProject(projectId));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long projectId, @PathVariable Long id,
                                    @RequestBody UpdateScriptRequest request) {
        scriptService.update(id, request.getContent());
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/generate")
    public ApiResponse<String> generate(@PathVariable Long projectId, @PathVariable Long id,
                                        @RequestBody GenerateScriptRequest request) {
        return ApiResponse.ok(scriptService.generate(id, request.getOutlineId()));
    }
}
