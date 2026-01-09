package com.lujianfeng.spanner.dto.user;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

/**
 * @author Lujianfeng
 * @version 1.0
 * @date 2026/1/8
 * @since 1.0
 */

@Component
@Getter
@Setter
public class UserRegisterRequestDTO {
    private String realName;

    private String password;

    private String avatarUrl;

    private String gender;

    private String email;

    private String phone;

    private String address;

    private Long age;

    @Override
    public String toString() {
        return "UserRegisterRequestDTO{" +
                "realName='" + realName + '\'' +
                ", password='" + password + '\'' +
                ", avatarUrl='" + avatarUrl + '\'' +
                ", gender='" + gender + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", address='" + address + '\'' +
                ", age=" + age +
                '}';
    }
}
