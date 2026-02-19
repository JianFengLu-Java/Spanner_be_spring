package com.lujianfeng.spanner.vo.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * @author mac
 */
@Component
@Getter
@Setter
public class UserVO {

    private Long userId;

    private String account;

    private String realName;

    private String avatarUrl;

    private String gender;

    private String email;

    private String phone;

    private String address;

    private String signature;

    private Long age;

    private LocalDateTime vipExpireAt;

    @JsonProperty("isVip")
    private Boolean isVip;

    private Long growthValue;

    private Integer vipLevel;

    private Integer userLevel;

}
