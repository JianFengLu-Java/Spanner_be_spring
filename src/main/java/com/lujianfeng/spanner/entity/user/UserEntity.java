package com.lujianfeng.spanner.entity.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * 用户实体类
 *
 * @author Lujianfeng
 * @version 1.0
 * @date 2025/12/15
 * @since 1.0
 */

@Setter
@Getter
@Entity
@Table(
        name = "user_info_test",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "account")
        }
)
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 账号：7-10 位数字，从 1000001 开始
     */
    @Column(nullable = false, length = 10)
    private String account;

    /**
     * 真实姓名
     */
    @Column(nullable = false, length = 50)
    private String realName;

    @Column(nullable = false)
    private String password;

    private String avatarUrl;
    private String gender;
    private String email;
    private String phone;
    private String address;
    private Long age;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<RoleEntity> roles = new HashSet<>();

    @Override
    public String toString() {
        return "UserEntity{" +
                "id=" + id +
                ", account='" + account + '\'' +
                ", realName='" + realName + '\'' +
                ", password='" + password + '\'' +
                ", avatarUrl='" + avatarUrl + '\'' +
                ", gender='" + gender + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", address='" + address + '\'' +
                ", age=" + age +
                ", roles=" + roles +
                '}';
    }
}