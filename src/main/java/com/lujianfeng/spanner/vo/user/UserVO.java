package com.lujianfeng.spanner.vo.user;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

/**
 * @author mac
 */
@Component
@Getter
@Setter
public class UserVO {

    private String account;

    private String realName;

    private String avatarUrl;

    private String gender;

    private String email;

    private String phone;

    private String address;

    private Long age;

}
