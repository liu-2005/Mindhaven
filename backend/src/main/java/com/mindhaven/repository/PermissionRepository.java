package com.mindhaven.repository;

import java.util.List;

import com.mindhaven.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    @Query("select p.code from Permission p " +
            "join RolePermission rp on rp.permissionId = p.id " +
            "join UserRole ur on ur.roleId = rp.roleId " +
            "where ur.userId = :userId")
    List<String> findCodesByUserId(@Param("userId") Long userId);
}
