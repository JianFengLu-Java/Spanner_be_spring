package com.lujianfeng.spanner.dto.user;

import lombok.Getter;
import lombok.Setter;

/**
 * 好友操作请求体
 */
@Getter
@Setter
public class FriendActionRequestDTO {
    private String friendAccount;
    private String verificationMessage;
}
