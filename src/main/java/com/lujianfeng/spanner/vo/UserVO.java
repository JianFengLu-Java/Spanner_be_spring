package com.lujianfeng.spanner.vo;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class UserVO {
    private String userName;

    private String avatarUrl;

    private String gender;

    private String email;

    private String phone;

    private String address;

    private Long age;
}
