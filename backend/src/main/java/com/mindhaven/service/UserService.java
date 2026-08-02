package com.mindhaven.service;

import java.time.LocalDateTime;
import java.util.List;

import com.mindhaven.common.BizException;
import com.mindhaven.common.ErrorCode;
import com.mindhaven.entity.Role;
import com.mindhaven.entity.User;
import com.mindhaven.entity.UserRole;
import com.mindhaven.repository.RoleRepository;
import com.mindhaven.repository.UserRepository;
import com.mindhaven.repository.UserRoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       UserRoleRepository userRoleRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(String username, String email, String password, String nickname, String phone,
                         String privacyConsentVersion) {
        if (!StringUtils.hasText(username) || username.length() < 3 || username.length() > 50) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "用户名长度需在 3-50 之间");
        }
        if (password == null || password.length() < 6) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "密码长度至少 6 位");
        }
        username = username.trim();
        email = email == null ? "" : email.trim().toLowerCase();

        if (userRepository.existsByUsername(username)) {
            throw new BizException(ErrorCode.RESOURCE_CONFLICT, "用户名已存在");
        }
        if (StringUtils.hasText(email) && userRepository.existsByEmail(email)) {
            throw new BizException(ErrorCode.RESOURCE_CONFLICT, "邮箱已被注册");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setNickname(StringUtils.hasText(nickname) ? nickname : username);
        user.setPhone(phone);
        user.setStatus("ACTIVE");
        if (StringUtils.hasText(privacyConsentVersion)) {
            user.setPrivacyConsentVersion(privacyConsentVersion);
            user.setConsentedAt(LocalDateTime.now());
        }
        user = userRepository.save(user);

        Role userRole = roleRepository.findByCode("USER")
                .orElseThrow(() -> new BizException(ErrorCode.INTERNAL_ERROR, "角色配置缺失"));
        userRoleRepository.save(new UserRole(user.getId(), userRole.getId()));
        return user;
    }

    public User findByUsernameOrEmail(String usernameOrEmail) {
        return userRepository.findByUsernameOrEmail(usernameOrEmail.trim(), usernameOrEmail.trim())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHENTICATED, "用户名或密码错误"));
    }

    public boolean matchesPassword(User user, String rawPassword) {
        return user != null && passwordEncoder.matches(rawPassword, user.getPasswordHash());
    }

    public User getById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Transactional
    public User updateProfile(Long userId, String nickname, String phone) {
        User user = getById(userId);
        if (StringUtils.hasText(nickname)) {
            user.setNickname(nickname.trim());
        }
        if (phone != null) {
            user.setPhone(phone);
        }
        return userRepository.save(user);
    }

    public List<String> rolesOf(Long userId) {
        return roleRepository.findCodesByUserId(userId);
    }
}
