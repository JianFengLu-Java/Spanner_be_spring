package com.lujianfeng.spanner.dto.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateProfileRequestDTO {

    private String realName;

    private String avatarUrl;

    private String gender;

    private String email;

    private String phone;

    private String address;

    private String signature;

    private Long age;
}
