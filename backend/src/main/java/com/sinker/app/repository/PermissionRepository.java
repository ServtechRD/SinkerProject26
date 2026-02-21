package com.sinker.app.repository;

import com.sinker.app.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    @Query(value = "SELECT p.code FROM permissions p " +
                   "INNER JOIN role_permissions rp ON rp.permission_id = p.id " +
                   "INNER JOIN roles r ON r.id = rp.role_id " +
                   "WHERE r.code = :roleCode AND p.is_active = true",
           nativeQuery = true)
    List<String> findPermissionCodesByRoleCode(@Param("roleCode") String roleCode);

    @Query(value = "SELECT p.* FROM permissions p " +
                   "INNER JOIN role_permissions rp ON rp.permission_id = p.id " +
                   "WHERE rp.role_id = :roleId " +
                   "ORDER BY p.module, p.code",
           nativeQuery = true)
    List<Permission> findByRoleId(@Param("roleId") Long roleId);
}
