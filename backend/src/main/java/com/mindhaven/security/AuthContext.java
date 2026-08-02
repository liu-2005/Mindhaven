package com.mindhaven.security;

import java.util.List;

public record AuthContext(Long userId, String username, List<String> roles) {

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
}
