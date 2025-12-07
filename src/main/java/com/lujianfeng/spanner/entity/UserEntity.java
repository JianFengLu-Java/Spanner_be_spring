package com.lujianfeng.spanner.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户实体类
 */
@Entity
@Setter
@Getter
@Table(name = "user_info_test")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Column(unique = true, nullable = false)
    private String userName;
    @Column
    private String password;
    @Column
    private String avatarUrl;
    @Column
    private String gender;
    @Column
    private String email;
    @Column
    private String phone;
    @Column
    private String address;
    @Column
    private Long age;
}
