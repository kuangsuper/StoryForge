package com.toonflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.toonflow.common.BizException;
import com.toonflow.common.ErrorCode;
import com.toonflow.common.PageResult;
import com.toonflow.dto.request.*;
import com.toonflow.dto.response.UserInfoResponse;
import com.toonflow.dto.response.UserQuotaResponse;
import com.toonflow.entity.User;
import com.toonflow.entity.UserQuota;
import com.toonflow.entity.UserRole;
import com.toonflow.mapper.UserMapper;
import com.toonflow.mapper.UserQuotaMapper;
import com.toonflow.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final UserQuotaMapper userQuotaMapper;
    private final PasswordEncoder passwordEncoder;

    public UserInfoResponse getMe(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BizException(ErrorCode.NOT_FOUND);
        UserRole role = userRoleMapper.selectOne(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        UserInfoResponse resp = new UserInfoResponse();
        resp.setId(user.getId());
        resp.setName(user.getName());
        resp.setEmail(user.getEmail());
        resp.setAvatar(user.getAvatar());
        resp.setStatus(user.getStatus());
        resp.setRole(role != null ? role.getRole() : "viewer");
        return resp;
    }

    public PageResult<UserInfoResponse> listUsers(int page, int size) {
        Page<User> p = userMapper.selectPage(new Page<>(page, size), null);
        List<Long> userIds = p.getRecords().stream().map(User::getId).toList();

        // 批量查询角色，避免 N+1
        Map<Long, String> roleMap = new java.util.HashMap<>();
        if (!userIds.isEmpty()) {
            List<UserRole> roles = userRoleMapper.selectList(
                    new LambdaQueryWrapper<UserRole>().in(UserRole::getUserId, userIds));
            for (UserRole role : roles) {
                roleMap.put(role.getUserId(), role.getRole());
            }
        }

        List<UserInfoResponse> records = p.getRecords().stream().map(u -> {
            UserInfoResponse r = new UserInfoResponse();
            r.setId(u.getId());
            r.setName(u.getName());
            r.setEmail(u.getEmail());
            r.setAvatar(u.getAvatar());
            r.setStatus(u.getStatus());
            r.setRole(roleMap.getOrDefault(u.getId(), "viewer"));
            return r;
        }).toList();
        return new PageResult<>(records, p.getTotal(), page, size);
    }

    @Transactional
    public void createUser(CreateUserRequest request) {
        User exists = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getName, request.getName()));
        if (exists != null) throw new BizException(400, "用户名已存在");
        User user = new User();
        user.setName(request.getName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setStatus(1);
        userMapper.insert(user);
        UserRole role = new UserRole();
        role.setUserId(user.getId());
        role.setRole("viewer");
        userRoleMapper.insert(role);
        UserQuota quota = new UserQuota();
        quota.setUserId(user.getId());
        quota.setDailyChapterLimit(50);
        quota.setDailyImageLimit(200);
        quota.setDailyVideoLimit(100);
        quota.setUsedChapters(0);
        quota.setUsedImages(0);
        quota.setUsedVideos(0);
        userQuotaMapper.insert(quota);
    }

    public void updateStatus(Long id, UpdateUserStatusRequest request) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BizException(ErrorCode.NOT_FOUND);
        user.setStatus(request.getStatus());
        userMapper.updateById(user);
    }

    public void deleteUser(Long id) {
        if (userMapper.selectById(id) == null) throw new BizException(ErrorCode.NOT_FOUND);
        userMapper.deleteById(id);
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, id));
        userQuotaMapper.delete(new LambdaQueryWrapper<UserQuota>().eq(UserQuota::getUserId, id));
    }

    public void updateRole(Long id, UpdateUserRoleRequest request) {
        if (userMapper.selectById(id) == null) throw new BizException(ErrorCode.NOT_FOUND);
        UserRole role = userRoleMapper.selectOne(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, id));
        if (role == null) {
            role = new UserRole();
            role.setUserId(id);
            role.setRole(request.getRole());
            userRoleMapper.insert(role);
        } else {
            role.setRole(request.getRole());
            userRoleMapper.updateById(role);
        }
    }

    public UserQuotaResponse getQuota(Long userId) {
        UserQuota quota = userQuotaMapper.selectOne(
                new LambdaQueryWrapper<UserQuota>().eq(UserQuota::getUserId, userId));
        if (quota == null) throw new BizException(ErrorCode.NOT_FOUND);
        UserQuotaResponse resp = new UserQuotaResponse();
        resp.setDailyChapterLimit(quota.getDailyChapterLimit());
        resp.setDailyImageLimit(quota.getDailyImageLimit());
        resp.setDailyVideoLimit(quota.getDailyVideoLimit());
        resp.setUsedChapters(quota.getUsedChapters());
        resp.setUsedImages(quota.getUsedImages());
        resp.setUsedVideos(quota.getUsedVideos());
        resp.setResetDate(quota.getResetDate());
        return resp;
    }

    public void updateQuota(Long userId, UpdateUserQuotaRequest request) {
        UserQuota quota = userQuotaMapper.selectOne(
                new LambdaQueryWrapper<UserQuota>().eq(UserQuota::getUserId, userId));
        if (quota == null) throw new BizException(ErrorCode.NOT_FOUND);
        if (request.getDailyChapterLimit() != null) quota.setDailyChapterLimit(request.getDailyChapterLimit());
        if (request.getDailyImageLimit() != null) quota.setDailyImageLimit(request.getDailyImageLimit());
        if (request.getDailyVideoLimit() != null) quota.setDailyVideoLimit(request.getDailyVideoLimit());
        userQuotaMapper.updateById(quota);
    }

    public void updatePassword(Long userId, UpdatePasswordRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BizException(ErrorCode.NOT_FOUND);
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BizException(400, "旧密码不正确");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
    }
}
