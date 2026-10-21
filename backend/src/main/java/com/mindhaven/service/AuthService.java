package com.mindhaven.service;

import java.time.LocalDateTime;
import java.util.List;

import com.mindhaven.common.BizException;
import com.mindhaven.common.ErrorCode;
import com.mindhaven.entity.User;
import com.mindhaven.repository.PermissionRepository;
import com.mindhaven.repository.UserRepository;
import com.mindhaven.security.AuthContext;
import com.mindhaven.security.LoginSessionService;
import com.mindhaven.security.PermissionService;
import org.springframework.stereotype.Service;

@Service
public class AuthService implements PermissionService {

    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final UserService userService;
    private final LoginSessionService sessions;

    public AuthService(UserRepository userRepository,
                       PermissionRepository permissionRepository,
                       UserService userService,
                       LoginSessionService sessions) {
        this.userRepository = userRepository;
        this.permissionRepository = permissionRepository;
        this.userService = userService;
        this.sessions = sessions;
    }

    public AuthContext login(String usernameOrEmail, String rawPassword) {
        User user = userService.findByUsernameOrEmail(usernameOrEmail);
        if (user.getDeletedAt() != null) {
            throw new BizException(ErrorCode.UNAUTHENTICATED, "用户名或密码错误");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BizException(ErrorCode.FORBIDDEN, "账户不可用");
        }
        if (!userService.matchesPassword(user, rawPassword)) {
            throw new BizException(ErrorCode.UNAUTHENTICATED, "用户名或密码错误");
        }
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        return new AuthContext(user.getId(), user.getUsername(), rolesOf(user.getId()));
    }

    public List<String> rolesOf(Long userId) {
        return userService.rolesOf(userId);
    }

    public String issueToken(AuthContext ctx) {
        return sessions.create(ctx.userId());
    }

    @Override
    public List<String> permissionsOf(Long userId) {
        return permissionRepository.findCodesByUserId(userId);
    }

    @Override
    public boolean hasAnyPermission(Long userId, String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return true;
        }
        List<String> owned = permissionsOf(userId);
        for (String p : permissions) {
            if (owned.contains(p)) {
                return true;
            }
        }
        return false;
    }
}
