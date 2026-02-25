package com.toonflow.security;

import com.toonflow.common.BizException;
import com.toonflow.common.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {

    private SecurityUtil() {}

    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return (Long) auth.getPrincipal();
    }

    public static String getCurrentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities().isEmpty()) {
            return "viewer";
        }
        return auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "").toLowerCase();
    }
}
