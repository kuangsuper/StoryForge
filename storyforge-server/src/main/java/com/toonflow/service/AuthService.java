package com.toonflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.toonflow.common.BizException;
import com.toonflow.common.ErrorCode;
import com.toonflow.dto.request.LoginRequest;
import com.toonflow.dto.response.LoginResponse;
import com.toonflow.entity.User;
import com.toonflow.entity.UserRole;
import com.toonflow.mapper.UserMapper;
import com.toonflow.mapper.UserRoleMapper;
import com.toonflow.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getName, request.getName()));
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        UserRole userRole = userRoleMapper.selectOne(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, user.getId()));
        String role = userRole != null ? userRole.getRole() : "viewer";
        String token = jwtTokenProvider.generateToken(user.getId(), user.getName(), role);
        return new LoginResponse(token, user.getId(), user.getName(), role);
    }

    public void logout(Long userId) {
        jwtTokenProvider.invalidateToken(userId);
    }
}
