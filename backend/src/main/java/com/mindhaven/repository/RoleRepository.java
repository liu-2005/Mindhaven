package com.mindhaven.repository;

import java.util.List;
import java.util.Optional;

import com.mindhaven.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByCode(String code);

    @Query("select r.code from Role r join UserRole ur on ur.roleId = r.id where ur.userId = :userId")
    List<String> findCodesByUserId(@Param("userId") Long userId);
}
