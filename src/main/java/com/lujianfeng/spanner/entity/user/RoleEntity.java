package com.lujianfeng.spanner.entity.user;

import com.lujianfeng.spanner.entity.premission.PermissionEntity;
import jakarta.persistence.*;
import lombok.Data;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 角色实体类
 *
 * @author Lujianfeng
 * @version 1.0
 * @date 2025/12/15
 * @since 1.0
 */
@Data
@Entity
@Table(name = "role_info")
public class RoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String roleName;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "role_permission",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )


    private Set<PermissionEntity> permissions = new HashSet<>();

    public Set<PermissionEntity> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<PermissionEntity> permissions) {
        this.permissions = permissions;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RoleEntity that = (RoleEntity) o;
        return Objects.equals(id, that.id) && Objects.equals(roleName, that.roleName) && Objects.equals(permissions, that.permissions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, roleName, permissions);
    }
}
