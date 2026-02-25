package com.toonflow.controller;

import com.toonflow.common.ApiResponse;
import com.toonflow.common.PageResult;
import com.toonflow.dto.request.*;
import com.toonflow.dto.response.UserInfoResponse;
import com.toonflow.dto.response.UserQuotaResponse;
import com.toonflow.security.SecurityUtil;
import com.toonflow.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserInfoResponse> getMe() {
        return ApiResponse.ok(userService.getMe(SecurityUtil.getCurrentUserId()));
    }

    @PutMapping("/me/password")
    public ApiResponse<Void> updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        userService.updatePassword(SecurityUtil.getCurrentUserId(), request);
        return ApiResponse.ok();
    }

    @GetMapping
    public ApiResponse<PageResult<UserInfoResponse>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(userService.listUsers(page, size));
    }

    @PostMapping
    public ApiResponse<Void> createUser(@Valid @RequestBody CreateUserRequest request) {
        userService.createUser(request);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id,
                                           @Valid @RequestBody UpdateUserStatusRequest request) {
        userService.updateStatus(id, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/role")
    public ApiResponse<Void> updateRole(@PathVariable Long id,
                                         @Valid @RequestBody UpdateUserRoleRequest request) {
        userService.updateRole(id, request);
        return ApiResponse.ok();
    }

    @GetMapping("/{id}/quota")
    public ApiResponse<UserQuotaResponse> getQuota(@PathVariable Long id) {
        return ApiResponse.ok(userService.getQuota(id));
    }

    @PutMapping("/{id}/quota")
    public ApiResponse<Void> updateQuota(@PathVariable Long id,
                                          @Valid @RequestBody UpdateUserQuotaRequest request) {
        userService.updateQuota(id, request);
        return ApiResponse.ok();
    }
}
