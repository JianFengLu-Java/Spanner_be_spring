package com.lujianfeng.spanner.entity.premission;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * @author Lujianfeng
 * @version 1.0
 * @date 2025/12/15
 * @since 1.0
 */

@Setter
@Getter
@Entity
@Table(name = "permission")
public class PermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String permission;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PermissionEntity that = (PermissionEntity) o;
        return Objects.equals(id, that.id) && Objects.equals(permission, that.permission);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, permission);
    }
}
