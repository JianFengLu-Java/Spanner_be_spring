package com.lujianfeng.spanner.vo.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lujianfeng.spanner.entity.user.UserRelationEnum;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 好友关系返回数据
 */
@Getter
@Builder
public class FriendRelationVO {
    private String account;
    private String realName;
    private String avatarUrl;
    private Boolean online;
    private String region;
    private String email;
    private String phone;
    private String gender;
    private String signature;
    private Long age;
    @JsonProperty("isVip")
    private Boolean isVip;
    private Long growthValue;
    private Integer vipLevel;
    private UserRelationEnum relationType;
    private String verificationMessage;
    private LocalDateTime createTime;
}
