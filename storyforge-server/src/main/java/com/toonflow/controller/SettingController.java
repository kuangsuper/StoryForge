package com.toonflow.controller;

import com.toonflow.common.ApiResponse;
import com.toonflow.dto.request.UpdateSettingRequest;
import com.toonflow.entity.Setting;
import com.toonflow.service.SettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system-settings")
@RequiredArgsConstructor
public class SettingController {

    private final SettingService settingService;

    @GetMapping
    public ApiResponse<Setting> get() {
        return ApiResponse.ok(settingService.get());
    }

    @PutMapping
    public ApiResponse<Void> update(@Valid @RequestBody UpdateSettingRequest request) {
        settingService.update(request);
        return ApiResponse.ok();
    }
}
