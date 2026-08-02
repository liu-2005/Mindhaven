package com.mindhaven.controller;

import com.mindhaven.common.ApiResponse;
import com.mindhaven.common.PageResult;
import com.mindhaven.dto.UserDto;
import com.mindhaven.entity.User;
import com.mindhaven.repository.UserRepository;
import com.mindhaven.security.RequirePermission;
import com.mindhaven.service.AuthService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final UserRepository userRepository;
    private final AuthService authService;

    public AdminUserController(UserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @GetMapping
    @RequirePermission("user:read")
    public ApiResponse<PageResult<UserDto>> page(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "20") int size,
                                                 @RequestParam(required = false) String keyword) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(Math.max(page, 1) - 1, safeSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<User> result = userRepository.search(keyword, pageable);
        java.util.List<UserDto> items = result.getContent().stream()
                .map(u -> UserDto.of(u, authService.rolesOf(u.getId()), authService.permissionsOf(u.getId())))
                .toList();
        return ApiResponse.success(PageResult.of(items, page, safeSize, result.getTotalElements()));
    }
}
