package com.mindhaven.security;

import java.util.List;

public interface PermissionService {

    List<String> permissionsOf(Long userId);

    boolean hasAnyPermission(Long userId, String... permissions);
}
