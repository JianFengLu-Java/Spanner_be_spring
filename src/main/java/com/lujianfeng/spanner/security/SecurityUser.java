package com.lujianfeng.spanner.security;

import com.lujianfeng.spanner.entity.premission.PermissionEntity;
import com.lujianfeng.spanner.entity.user.RoleEntity;
import com.lujianfeng.spanner.entity.user.UserEntity;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


public record SecurityUser(UserEntity userEntity) implements UserDetails {

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }

    @Override
    public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // 遍历用户所有角色
        for (RoleEntity role : userEntity.getRoles()) {

            // 添加角色
            authorities.add(new SimpleGrantedAuthority(role.getRoleName()));

            // 如果角色有权限，再添加权限（可选）
            for (PermissionEntity perm : role.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(perm.getPermission()));
            }
        }

        return authorities;
    }

    @Override
    public @Nullable String getPassword() {
        return userEntity.getPassword();
    }

    @Override
    public @NonNull String getUsername() {
        return userEntity.getUserName();
    }
}
