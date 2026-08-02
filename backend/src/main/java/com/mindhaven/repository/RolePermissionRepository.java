package com.mindhaven.repository;

import com.mindhaven.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermission.RolePermissionId> {
}
