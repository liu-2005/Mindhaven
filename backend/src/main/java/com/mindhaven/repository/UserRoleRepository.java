package com.mindhaven.repository;

import com.mindhaven.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRole.UserRoleId> {

    boolean existsByUserId(Long userId);
}
