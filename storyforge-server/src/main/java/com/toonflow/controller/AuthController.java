package com.toonflow.controller;

import com.toonflow.common.ApiResponse;
import com.toonflow.dto.request.LoginRequest;
import com.toonflow.dto.response.LoginResponse;
import com.toonflow.security.SecurityUtil;
import com.toonflow.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout(SecurityUtil.getCurrentUserId());
        return ApiResponse.ok();
    }
}
